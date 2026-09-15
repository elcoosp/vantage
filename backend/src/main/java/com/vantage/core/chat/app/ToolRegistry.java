package com.vantage.core.chat.app;

import com.vantage.core.chat.app.tool.ChatTool;
import com.vantage.core.chat.app.LlmProvider.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Collects all available {@link ChatTool} instances and scopes them to a tenant.
 * The orchestrator calls {@link #definitions(UUID)} to get the tool list for the
 * prompt, then {@link #execute(UUID, String, Map)} to dispatch a tool call.
 */
@Component
public class ToolRegistry {

    private final List<ChatTool> tools;

    public ToolRegistry(List<ChatTool> tools) {
        this.tools = tools;
    }

    public List<ToolDefinition> definitions(UUID tenantId) {
        return tools.stream()
            .map(ChatTool::definition)
            .collect(Collectors.toList());
    }

    public String execute(UUID tenantId, String toolName, Map<String, Object> arguments) {
        for (ChatTool tool : tools) {
            if (tool.definition().name().equals(toolName)) {
                return tool.execute(tenantId, arguments);
            }
        }
        return "Tool \"" + toolName + "\" is not available.";
    }
}
