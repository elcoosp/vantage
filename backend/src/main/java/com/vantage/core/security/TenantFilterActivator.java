package com.vantage.core.security;

import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Per-request activator that enables the per-entity Hibernate {@code tenantFilter_*} filters
 * whenever a tenant context is present. This complements {@link com.vantage.core.tenant.TenantFilterInterceptor}
 * (which enables them at transaction begin) and guarantees they are active for the very first
 * query of a request.
 */
@Component
public class TenantFilterActivator extends OncePerRequestFilter {

    static final List<String> TENANT_FILTER_NAMES = List.of(
            "tenantFilter_Order",
            "tenantFilter_StorefrontConfig",
            "tenantFilter_EntityEvent",
            "tenantFilter_OutboxEvent",
            "tenantFilter_ApiKey",
            "tenantFilter_IdempotencyKey",
            "tenantFilter_Product",
            "tenantFilter_Inventory",
            "tenantFilter_Vendor");

    private final EntityManager entityManager;

    public TenantFilterActivator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            for (String filterName : TENANT_FILTER_NAMES) {
                if (session.getEnabledFilter(filterName) == null) {
                    session.enableFilter(filterName).setParameter("tenantId", tenantId);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
