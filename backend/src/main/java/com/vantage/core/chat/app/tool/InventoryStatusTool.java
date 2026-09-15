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
public class InventoryStatusTool implements ChatTool {

    private final EntityManager entityManager;

    public InventoryStatusTool(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "getInventory",
            "Check current stock level for a product in the current tenant.",
            """
            {
              "type": "object",
              "properties": {
                "productId": {"type": "string", "description": "The UUID of the product to check."}
              },
              "required": ["productId"]
            }
            """
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(UUID tenantId, Map<String, Object> arguments) {
        UUID resolvedTenant = resolveTenant(tenantId);
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
        // Scoped by tenant + product — no cross-tenant leakage.
        var result = entityManager.createNativeQuery(
                """
                SELECT i.quantity, p.name
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                WHERE i.product_id = :productId AND i.tenant_id = :tenantId
                """)
            .setParameter("productId", productId)
            .setParameter("tenantId", resolvedTenant)
            .getResultList();
        if (result.isEmpty()) {
            return "Product " + productId + " not found for this tenant.";
        }
        Object[] row = (Object[]) result.get(0);
        int qty = ((Number) row[0]).intValue();
        String name = (String) row[1];
        return String.format("Product \"%s\" (id=%s): stock level = %d units", name, productId, qty);
    }

    private UUID resolveTenant(UUID tenantId) {
        if (tenantId != null) return tenantId;
        UUID ctx = TenantContext.getTenantId();
        if (ctx == null) throw new MissingTenantContextException(
            "No tenant context for getInventory tool");
        return ctx;
    }
}
