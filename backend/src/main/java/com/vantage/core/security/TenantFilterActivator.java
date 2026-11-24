package com.vantage.core.security;

import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
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
 *
 * <p>Spring Boot 4 dropped {@code @PersistenceContext} injection of the transaction-scoped
 * {@code EntityManager}, so the {@code EntityManagerFactory} is injected instead and a fresh
 * manager is obtained per request. The filters are enabled on the Hibernate {@code Session}
 * obtained from that manager; the manager is closed when the request finishes.</p>
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
            "tenantFilter_Vendor",
            "tenantFilter_AiConversation",
            "tenantFilter_AiMessage",
            "tenantFilter_AiToolCall",
            "tenantFilter_AiUsage",
            "tenantFilter_AiDocument");

    private final EntityManagerFactory entityManagerFactory;

    public TenantFilterActivator(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        EntityManager entityManager = null;
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                entityManager = entityManagerFactory.createEntityManager();
                Session session = entityManager.unwrap(Session.class);
                for (String filterName : TENANT_FILTER_NAMES) {
                    if (session.getEnabledFilter(filterName) == null) {
                        session.enableFilter(filterName).setParameter("tenantId", tenantId);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}
