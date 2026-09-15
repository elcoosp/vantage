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
public class ProductSearchTool implements ChatTool {

    private final EntityManager entityManager;

    public ProductSearchTool(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "searchProducts",
            "Search for products by name or SKU in the current tenant's catalog.",
            """
            {
              "type": "object",
              "properties": {
                "query": {"type": "string", "description": "Search term for product name or SKU."},
                "limit": {"type": "integer", "description": "Maximum number of results to return (default 10)."}
              },
              "required": ["query"]
            }
            """
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(UUID tenantId, Map<String, Object> arguments) {
        UUID resolvedTenant = resolveTenant(tenantId);
        String query = (String) arguments.get("query");
        if (query == null || query.isBlank()) {
            return "Missing required parameter: query";
        }
        int limit = arguments.get("limit") != null
            ? Math.min(((Number) arguments.get("limit")).intValue(), 50)
            : 10;
        var results = entityManager.createNativeQuery(
                """
                SELECT p.id, p.name, p.sku, p.price
                FROM products p
                WHERE p.tenant_id = :tenantId
                  AND (p.name ILIKE :searchTerm OR p.sku ILIKE :searchTerm)
                ORDER BY p.name
                LIMIT :limit
                """)
            .setParameter("tenantId", resolvedTenant)
            .setParameter("searchTerm", "%" + query + "%")
            .setParameter("limit", limit)
            .getResultList();
        if (results.isEmpty()) {
            return "No products found matching \"" + query + "\" for this tenant.";
        }
        StringBuilder sb = new StringBuilder();
        for (Object row : results) {
            Object[] r = (Object[]) row;
            sb.append(String.format("[id=%s, name=\"%s\", sku=%s, price=%s]; ",
                r[0], r[1], r[2], r[3]));
        }
        return sb.toString();
    }

    private UUID resolveTenant(UUID tenantId) {
        if (tenantId != null) return tenantId;
        UUID ctx = TenantContext.getTenantId();
        if (ctx == null) throw new MissingTenantContextException(
            "No tenant context for searchProducts tool");
        return ctx;
    }
}
