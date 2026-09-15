package com.vantage.core.chat.app.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.chat.app.LlmProvider;
import com.vantage.core.chat.app.LlmStreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible LLM provider that streams tokens over HTTP.
 * Supports OpenAI, Anthropic (via OpenAI-compatible proxy), and Ollama.
 *
 * <p>When configured with provider "stub" or no API key, falls back to a
 * deterministic canned response so the streaming pipeline can be tested
 * without an LLM. This is NOT production behavior.
 */
@Service
@Slf4j
public class OpenAiProvider implements LlmProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String providerName;
    private final boolean realProvider;

    public OpenAiProvider(
            @Value("${vantage.ai.provider:openapi}") String provider,
            @Value("${vantage.ai.openai.api-key:}") String apiKey,
            @Value("${vantage.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${vantage.ai.openai.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper) {
        this.model = model;
        this.providerName = provider;
        this.objectMapper = objectMapper;
        this.realProvider = "stub".equals(provider) || apiKey == null || apiKey.isBlank();
        this.webClient = realProvider ? null : WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public List<LlmStreamEvent> stream(LlmRequest request) {
        log.debug("[{}] Streaming with {} tools for tenant {}",
            providerName, request.tools().size(), request.tenantParams().get("tenant"));

        if (realProvider) {
            return stubResponse(request);
        }

        // Build the OpenAI-compatible chat completion request
        Map<String, Object> openAiRequest = new LinkedHashMap<>();
        openAiRequest.put("model", model);
        openAiRequest.put("stream", true);
        openAiRequest.put("max_tokens", request.maxTokens());
        openAiRequest.put("temperature", request.temperature());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        for (ChatMessage msg : request.messages()) {
            messages.add(Map.of(
                "role", msg.role().value(),
                "content", msg.content()
            ));
        }
        openAiRequest.put("messages", messages);

        if (!request.tools().isEmpty()) {
            List<Map<String, Object>> tools = request.tools().stream()
                .map(t -> Map.of(
                    "type", "function",
                    "function", Map.of(
                        "name", t.name(),
                        "description", t.description(),
                        "parameters", t.parametersJsonSchema()
                    )
                ))
                .collect(Collectors.toList());
            openAiRequest.put("tools", tools);
            openAiRequest.put("tool_choice", "auto");
        }

        return webClient.post()
            .uri("/chat/completions")
            .body(BodyInserters.fromValue(openAiRequest))
            .retrieve()
            .bodyToFlux(String.class)
            .timeout(Duration.ofSeconds(60))
            .flatMapSequential(this::parseStreamLine)
            .collectList()
            .onErrorResume(e -> {
                log.error("LLM provider error", e);
                return Mono.just(List.of(new LlmStreamEvent.Error(
                    "LLM provider error: " + e.getMessage(), "PROVIDER_ERROR")));
            })
            .block();
    }

    private List<LlmStreamEvent> stubResponse(LlmRequest request) {
        List<LlmStreamEvent> events = new ArrayList<>();

        String lastUserMsg = request.messages().getLast().content();
        String lower = lastUserMsg != null ? lastUserMsg.toLowerCase() : "";

        if (lower.contains("order")) {
            events.add(new LlmStreamEvent.ToolCall(
                UUID.randomUUID().toString(),
                "getOrderStatus",
                "{\"orderId\": \"00000000-0000-0000-0000-000000000000\"}",
                false
            ));
        } else if (lower.contains("inventory") || lower.contains("stock")) {
            events.add(new LlmStreamEvent.ToolCall(
                UUID.randomUUID().toString(),
                "getInventory",
                "{\"productId\": \"00000000-0000-0000-0000-000000000000\"}",
                false
            ));
        } else if (lower.contains("forecast") || lower.contains("predict")) {
            events.add(new LlmStreamEvent.ToolCall(
                UUID.randomUUID().toString(),
                "getForecast",
                "{\"productId\": \"00000000-0000-0000-0000-000000000000\"}",
                false
            ));
        } else {
            String[] words = ("I can help you with order status, " +
                "inventory checks, product search, and demand forecasts. " +
                "Please let me know what you need!").split(" ");
            for (String word : words) {
                events.add(new LlmStreamEvent.ContentToken(word + " "));
            }
        }
        events.add(new LlmStreamEvent.Usage(10, 20, 30, 0.001, model));
        return events;
    }

    private reactor.core.publisher.Flux<LlmStreamEvent> parseStreamLine(String line) {
        List<LlmStreamEvent> events = new ArrayList<>();
        if (line == null || line.isBlank()) return reactor.core.publisher.Flux.fromIterable(events);

        line = line.trim();
        if (line.startsWith("data:")) {
            String data = line.substring(5).trim();
            if ("[DONE]".equals(data)) {
                events.add(new LlmStreamEvent.Done());
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                    List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) chunk.getOrDefault("choices", List.of());
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> delta =
                            (Map<String, Object>) choice.getOrDefault("delta", Map.of());
                        String content = (String) delta.get("content");
                        if (content != null && !content.isBlank()) {
                            events.add(new LlmStreamEvent.ContentToken(content));
                        }
                        List<Map<String, Object>> toolCalls =
                            (List<Map<String, Object>>) delta.get("tool_calls");
                        if (toolCalls != null) {
                            for (Map<String, Object> tc : toolCalls) {
                                String id = (String) tc.get("id");
                                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                                String name = fn != null ? (String) fn.get("name") : null;
                                String args = fn != null ? (String) fn.get("arguments") : null;
                                events.add(new LlmStreamEvent.ToolCall(id, name, args, true));
                            }
                        }
                    }
                    Map<String, Object> usage = (Map<String, Object>) chunk.get("usage");
                    if (usage != null) {
                        events.add(new LlmStreamEvent.Usage(
                            (Integer) usage.getOrDefault("prompt_tokens", 0),
                            (Integer) usage.getOrDefault("completion_tokens", 0),
                            (Integer) usage.getOrDefault("total_tokens", 0),
                            (Double) usage.getOrDefault("cost", 0.0),
                            model
                        ));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse SSE line: {}", data, e);
                }
            }
        }
        return reactor.core.publisher.Flux.fromIterable(events);
    }
}
