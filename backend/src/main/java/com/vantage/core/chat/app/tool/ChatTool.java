package com.vantage.core.chat.app.tool;

import com.vantage.core.chat.app.LlmProvider;
import com.vantage.core.chat.app.LlmProvider.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A callable function exposed to the LLM. Every tool is constructed with a
 * tenantId and must validate every input against {@link TenantContext} so that
 * a prompt-injection or cross-tenant query can never leak data.
 */
public interface ChatTool {
    ToolDefinition definition();

    /**
     * Execute the tool for the given tenant and arguments.
     * Implementations must throw {@link com.vantage.core.tenant.MissingTenantContextException}
     * if the tenant context is missing, and must scope every query by tenantId.
     */
    String execute(UUID tenantId, Map<String, Object> arguments);

    record SimpleTool(
        String name,
        String description,
        String parametersJsonSchema,
        java.util.function.BiFunction<UUID, Map<String, Object>, String> handler
    ) implements ChatTool {
        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name, description, parametersJsonSchema);
        }

        @Override
        public String execute(UUID tenantId, Map<String, Object> arguments) {
            return handler.apply(tenantId, arguments);
        }
    }
}
