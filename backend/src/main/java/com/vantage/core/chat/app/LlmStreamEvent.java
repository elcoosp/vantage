package com.vantage.core.chat.app;

import java.util.Map;

/**
 * Events emitted by {@link LlmProvider#stream}. The controller serializes each
 * event as a single SSE line of JSON so the frontend can differentiate content
 * tokens from tool calls, citations, and usage.
 *
 * <p>Polymorphism is handled manually via the {@code type} field in each record.
 * The Jackson {@code @JsonTypeInfo} / {@code @JsonSubTypes} annotations are not
 * needed because serialization is performed by the controller's ObjectMapper
 * on a plain record — the {@code type} field is set explicitly in the frontend
 * or by the provider before serialization.
 */
public sealed interface LlmStreamEvent permits
    LlmStreamEvent.ContentToken,
    LlmStreamEvent.ToolCall,
    LlmStreamEvent.ToolResult,
    LlmStreamEvent.Citation,
    LlmStreamEvent.Usage,
    LlmStreamEvent.Error,
    LlmStreamEvent.Done {

    record ContentToken(String text, Integer tokenCount, String type) implements LlmStreamEvent {
        public ContentToken(String text) { this(text, null, "content"); }
        public ContentToken(String text, Integer tokenCount) { this(text, tokenCount, "content"); }
    }

    record ToolCall(String id, String name, String arguments, Boolean partial, String type) implements LlmStreamEvent {
        public ToolCall(String id, String name, String arguments, Boolean partial) {
            this(id, name, arguments, partial, "tool_call");
        }
    }

    record ToolResult(String toolCallId, String content, Map<String, Object> data, String type) implements LlmStreamEvent {
        public ToolResult(String toolCallId, String content, Map<String, Object> data) {
            this(toolCallId, content, data, "tool_result");
        }
    }

    /**
     * A citation to a RAG-retrieved document chunk. The frontend renders
     * these in a sidebar or inline as reference links.
     */
    record Citation(String documentTitle, String snippet, String sourceUrl, Integer chunkIndex, String type) implements LlmStreamEvent {
        public Citation(String documentTitle, String snippet, String sourceUrl, Integer chunkIndex) {
            this(documentTitle, snippet, sourceUrl, chunkIndex, "citation");
        }
    }

    record Usage(Integer promptTokens, Integer completionTokens, Integer totalTokens, Double cost, String model, String type) implements LlmStreamEvent {
        public Usage(Integer promptTokens, Integer completionTokens, Integer totalTokens, Double cost, String model) {
            this(promptTokens, completionTokens, totalTokens, cost, model, "usage");
        }
    }

    record Error(String message, String code, String type) implements LlmStreamEvent {
        public Error(String message, String code) {
            this(message, code, "error");
        }
    }

    record Done(String type) implements LlmStreamEvent {
        public Done() { this("done"); }
    }
}
