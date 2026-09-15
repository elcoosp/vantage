package com.vantage.core.chat.app;

import com.vantage.core.chat.infra.AiConversation;
import com.vantage.core.chat.infra.AiMessage;
import com.vantage.core.chat.infra.ConversationRepository;
import com.vantage.core.chat.infra.DocumentRetriever;
import com.vantage.core.chat.infra.RagContext;
import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Core orchestrator for the AI support chat.
 *
 * <p>Replaces the old canned {@code ChatService.getResponseWords()} stub with:
 * <ul>
 *   <li>LLM provider abstraction (OpenAI / Anthropic / Ollama)</li>
 *   <li>Multi-turn conversation persistence</li>
 *   <li>Tool/function-calling with tenant-isolated tools</li>
 *   <li>RAG retrieval from help docs via {@link DocumentRetriever}</li>
 *   <li>Guardrails: PII redaction, prompt-injection detection, per-tenant rate limiting</li>
 *   <li>Token-stream SSE emission via {@link LlmStreamEvent}</li>
 * </ul>
 */
@Service
@Slf4j
public class ChatOrchestrator {

    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final AiGuardrails guardrails;
    private final TokenRateLimiter rateLimiter;
    private final ConversationRepository conversationRepository;
    private final DocumentRetriever retriever;
    private final ObjectMapper objectMapper;

    public ChatOrchestrator(
            LlmProvider llmProvider,
            ToolRegistry toolRegistry,
            AiGuardrails guardrails,
            TokenRateLimiter rateLimiter,
            ConversationRepository conversationRepository,
            DocumentRetriever retriever,
            ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.guardrails = guardrails;
        this.rateLimiter = rateLimiter;
        this.conversationRepository = conversationRepository;
        this.retriever = retriever;
        this.objectMapper = objectMapper;
    }

    /**
     * Stream chat response events for the given query, optionally continuing an
     * existing conversation. Returns the full list of events; the controller
     * serializes each as an SSE line.
     *
     * @param query          The user's message text.
     * @param conversationId Optional existing conversation UUID (null for new).
     * @return List of stream events (content tokens, tool calls, tool results,
     *         citations, usage, errors).
     */
    public List<LlmStreamEvent> stream(String query, UUID conversationId) {
        UUID tenantId = requireTenant();
        String redactedQuery = guardrails.redactPii(query);

        if (guardrails.detectPromptInjection(redactedQuery)) {
            log.warn("Prompt injection detected for tenant {}", tenantId);
            return List.of(new LlmStreamEvent.Error(
                "I'm sorry, but I cannot process that request.", "PROMPT_INJECTION"));
        }

        if (!rateLimiter.tryConsume(tenantId, 100)) {
            log.warn("Rate limit exceeded for tenant {}", tenantId);
            return List.of(new LlmStreamEvent.Error(
                "Rate limit exceeded. Please try again later.", "RATE_LIMITED"));
        }

        AiConversation conv = conversationId != null
            ? conversationRepository.findById(conversationId)
            : null;
        if (conv == null) {
            String title = query.length() > 50 ? query.substring(0, 50) : query;
            conv = conversationRepository.create(title);
        }

        // RAG: retrieve relevant docs from the same tenant's knowledge base
        List<RagContext> context = retriever.retrieve(redactedQuery, 5);
        String systemPrompt = guardrails.buildSystemPrompt(tenantId);
        List<LlmStreamEvent> processed = new ArrayList<>();

        if (!context.isEmpty()) {
            // Emit citation events so the frontend can render reference links
            for (RagContext ctx : context) {
                processed.add(new LlmStreamEvent.Citation(
                    ctx.title(), ctx.content(), ctx.sourceUrl(), 0));
            }

            // Inject retrieved context into the system prompt
            StringBuilder contextBuilder = new StringBuilder("\n\nRelevant context from knowledge base:\n");
            for (RagContext ctx : context) {
                contextBuilder.append("- ").append(ctx.content()).append("\n");
            }
            systemPrompt += contextBuilder.toString();
        }

        // Build message list: system prompt + history + user query
        List<LlmProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new LlmProvider.ChatMessage(ChatMessageRole.SYSTEM, systemPrompt));

        for (AiMessage msg : conv.getMessages()) {
            messages.add(toLlmMessage(msg));
        }
        messages.add(new LlmProvider.ChatMessage(ChatMessageRole.USER, redactedQuery));

        // Persist the user message
        persistUserMessage(tenantId, conv, redactedQuery);

        // Get events from the LLM provider
        List<LlmStreamEvent> events = llmProvider.stream(new LlmProvider.LlmRequest(
            systemPrompt,
            messages,
            toolRegistry.definitions(tenantId),
            Map.of("tenant", tenantId.toString()),
            guardrails.getMaxTokensPerTenant(),
            0.7
        ));

        // Execute tool calls inline — provider returns ToolCall events, we
        // emit them, execute them, and inject ToolResult + ContentToken events
        boolean hasDone = false;
        for (LlmStreamEvent event : events) {
            if (event instanceof LlmStreamEvent.ToolCall tc) {
                processed.add(event);
                try {
                    Map<String, Object> args = parseArgs(tc.arguments());
                    String toolResult = toolRegistry.execute(tenantId, tc.name(), args);
                    toolResult = guardrails.redactPii(toolResult);
                    processed.add(new LlmStreamEvent.ToolResult(tc.id(), toolResult, null));
                    processed.add(new LlmStreamEvent.ContentToken(
                        "[Tool: " + tc.name() + "] " + toolResult));
                } catch (Exception e) {
                    log.error("Tool {} failed for tenant {}", tc.name(), tenantId, e);
                    processed.add(new LlmStreamEvent.Error(
                        "Tool " + tc.name() + " failed: " + e.getMessage(), "TOOL_ERROR"));
                }
            } else if (event instanceof LlmStreamEvent.Done) {
                hasDone = true;
                processed.add(event);
            } else {
                processed.add(event);
            }
        }
        if (!hasDone) {
            processed.add(new LlmStreamEvent.Done());
        }
        return processed;
    }

    private void persistUserMessage(UUID tenantId, AiConversation conv, String content) {
        try {
            AiMessage userMsg = new AiMessage();
            userMsg.setConversation(conv);
            userMsg.setTenantId(tenantId);
            userMsg.setRole(AiMessage.AiMessageRole.USER);
            userMsg.setContent(content);
            conv.getMessages().add(userMsg);
        } catch (Exception e) {
            log.warn("Failed to persist user message for tenant {}", tenantId, e);
        }
    }

    private LlmProvider.ChatMessage toLlmMessage(AiMessage msg) {
        return switch (msg.getRole()) {
            case USER -> new LlmProvider.ChatMessage(ChatMessageRole.USER, msg.getContent());
            case ASSISTANT -> new LlmProvider.ChatMessage(ChatMessageRole.ASSISTANT, msg.getContent());
            case SYSTEM -> new LlmProvider.ChatMessage(ChatMessageRole.SYSTEM, msg.getContent());
            case TOOL -> new LlmProvider.ChatMessage(
                ChatMessageRole.TOOL, msg.getContent(), msg.getToolName(), msg.getToolCallId());
        };
    }

    private Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return Collections.emptyMap();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(argsJson, Map.class);
            return result;
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("No tenant context for chat stream");
        }
        return tenantId;
    }
}
