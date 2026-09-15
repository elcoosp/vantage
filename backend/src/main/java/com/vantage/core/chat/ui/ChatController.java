package com.vantage.core.chat.ui;

import com.vantage.core.chat.app.ChatOrchestrator;
import com.vantage.core.chat.app.LlmStreamEvent;
import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the streaming AI support chat.
 *
 * <p>Replaces the old canned SSE endpoint with a real orchestrated LLM stream.
 * The endpoint accepts a POST body with the user's message and an optional
 * conversation ID (to continue a thread). It streams {@code text/event-stream}
 * SSE lines, each a JSON-serialized {@link LlmStreamEvent}.
 *
 * <p>Tenant is resolved from the JWT token or X-Tenant-ID header by
 * {@link com.vantage.core.tenant.TenantFilter} — no cross-tenant leakage is
 * possible.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Slf4j
public class ChatController {

    private final ChatOrchestrator chatOrchestrator;
    private final ObjectMapper objectMapper;

    public ChatController(ChatOrchestrator chatOrchestrator, ObjectMapper objectMapper) {
        this.chatOrchestrator = chatOrchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * @param request Body with the user query and optional conversation ID.
     * @return SSE stream of JSON event lines.
     */
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.reactivestreams.Publisher<String> stream(@RequestBody ChatRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("Tenant context required for chat stream");
        }
        log.info("Chat stream started for tenant {} (conversation: {})",
            tenantId, request.conversationId());

        return reactor.core.publisher.Flux.fromIterable(
                chatOrchestrator.stream(request.query(), request.conversationId()))
            .map(event -> {
                try {
                    return "data: " + objectMapper.writeValueAsString(event) + "\n\n";
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize SSE event", e);
                    return "data: {\"type\":\"error\",\"message\":\"Serialization failed\"}\n\n";
                }
            })
            .doOnComplete(() -> log.info("Chat stream completed for tenant {}", TenantContext.getTenantId()))
            .doOnError(e -> log.error("Chat stream error for tenant {}", TenantContext.getTenantId(), e));
    }

    public record ChatRequest(String query, UUID conversationId) {}
}
