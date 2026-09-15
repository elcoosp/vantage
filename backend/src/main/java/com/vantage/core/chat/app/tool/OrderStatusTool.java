package com.vantage.core.chat.app.tool;

import com.vantage.core.chat.app.LlmProvider.ToolDefinition;
import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderStatusTool implements ChatTool {

    private final EntityManager entityManager;

    public OrderStatusTool(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "getOrderStatus",
            "Look up the status of an order for the current tenant by order ID.",
            """
            {
              "type": "object",
              "properties": {
                "orderId": {"type": "string", "description": "The UUID of the order to look up."}
              },
              "required": ["orderId"]
            }
            """
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(UUID tenantId, Map<String, Object> arguments) {
        UUID resolvedTenant = resolveTenant(tenantId);
        String orderIdStr = (String) arguments.get("orderId");
        if (orderIdStr == null) {
            return "Missing required parameter: orderId";
        }
        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdStr);
        } catch (IllegalArgumentException e) {
            return "Invalid orderId format";
        }
        // Query scoped by both tenantId and orderId — no cross-tenant leakage.
        var result = entityManager.createNativeQuery(
                """
                SELECT o.status, o.quantity, o.created_at
                FROM orders o
                WHERE o.id = :orderId AND o.tenant_id = :tenantId
                """)
            .setParameter("orderId", orderId)
            .setParameter("tenantId", resolvedTenant)
            .getResultList();
        if (result.isEmpty()) {
            return "Order " + orderId + " not found for this tenant.";
        }
        Object[] row = (Object[]) result.get(0);
        return String.format("Order %s: status=%s, quantity=%s, created_at=%s",
            orderId, row[0], row[1], row[2]);
    }

    private UUID resolveTenant(UUID tenantId) {
        if (tenantId != null) return tenantId;
        UUID ctx = TenantContext.getTenantId();
        if (ctx == null) throw new MissingTenantContextException(
            "No tenant context for getOrderStatus tool");
        return ctx;
    }
}
