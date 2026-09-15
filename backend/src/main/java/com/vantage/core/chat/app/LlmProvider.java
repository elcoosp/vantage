package com.vantage.core.chat.app;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstracts the LLM provider so the chat orchestrator is agnostic of vendor.
 * Implementations include OpenAiProvider, AnthropicProvider, and OllamaProvider.
 */
public interface LlmProvider {
    /**
     * Stream tokens (and tool-call / usage events) for a conversation.
     * Each item emitted is a serialized event string (typically JSON) that the
     * caller writes directly to the response stream.
     */
    List<LlmStreamEvent> stream(LlmRequest request);

    record LlmRequest(
        String systemPrompt,
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        Map<String, String> tenantParams,
        int maxTokens,
        double temperature
    ) {}

    record ChatMessage(
        ChatMessageRole role,
        String content,
        String toolCallId,
        String toolName,
        UUID messageId
    ) {
        public ChatMessage(ChatMessageRole role, String content) {
            this(role, content, null, null, UUID.randomUUID());
        }
        public ChatMessage(ChatMessageRole role, String content, String toolName, String toolCallId) {
            this(role, content, toolCallId, toolName, UUID.randomUUID());
        }
    }

    record ToolDefinition(
        String name,
        String description,
        String parametersJsonSchema
    ) {}
}
