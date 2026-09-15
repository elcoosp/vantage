package com.vantage.core.chat.app.tool;

import com.vantage.analytics.app.AnalyticsService;
import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;
import com.vantage.core.chat.app.LlmProvider.ToolDefinition;
import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ForecastTool implements ChatTool {

    private final AnalyticsService analyticsService;

    public ForecastTool(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "getForecast",
            "Get a 7-day demand forecast for a product in the current tenant. Returns predicted quantities with confidence intervals.",
            """
            {
              "type": "object",
              "properties": {
                "productId": {"type": "string", "description": "The UUID of the product to forecast."}
              },
              "required": ["productId"]
            }
            """
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> arguments) {
        // AnalyticsService already reads TenantContext internally.
        if (TenantContext.getTenantId() == null && tenantId == null) {
            throw new MissingTenantContextException("No tenant context for getForecast tool");
        }
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            String productIdStr = (String) arguments.get("productId");
            if (productIdStr == null) {
                return "Missing required parameter: productId";
            }
            UUID productId;
            try {
                productId = UUID.fromString(productIdStr);
            } catch (IllegalArgumentException e) {
                return "Invalid productId format";
            }
            ForecastResponse forecast = analyticsService.getForecast(productId);
            return forecast.forecast().stream()
                .map(p -> String.format("Day %s: predicted=%d, lower=%d, upper=%d",
                    p.date(), p.predictedQuantity(), p.lowerBound(), p.upperBound()))
                .collect(Collectors.joining("; ", "Forecast: ", ""));
        } finally {
            if (tenantId != null) {
                TenantContext.clear();
            }
        }
    }
}
