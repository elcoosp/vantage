package com.vantage.core.tenant;

import com.vantage.core.tenant.TenantContext;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Enables the per-entity Hibernate {@code tenantFilter_*} filters on the current persistence
 * session whenever a tenant context is present.
 *
 * <p>Unlike an AOP aspect (which cannot reliably advise Spring Data repository proxies), a
 * Hibernate {@link org.hibernate.Interceptor} is invoked by the persistence engine itself for
 * every transaction, so the filters are guaranteed to apply to every HQL/JPQL/Criteria query
 * (including {@code findAll()} and derived queries) within a tenant-scoped request.</p>
 *
 * <p>By-id lookups ({@code findById}/{@code getById}/{@code existsById}) are isolated via the
 * tenant-aware base repository {@link TenantJpaRepositoryImpl}, because Hibernate {@code @Filter}
 * is not applied to {@code EntityManager.find()} root-entity loads.</p>
 *
 * <p>The current {@link Session} is obtained from the {@link Transaction} that Hibernate passes
 * to {@link #afterTransactionBegin(Transaction)}. The concrete transaction implementation holds
 * a reference to its owning session; we resolve it reflectively to avoid a bean dependency on
 * the {@code EntityManagerFactory} (which would create a circular dependency during context
 * startup).</p>
 */
@Component
public class TenantFilterInterceptor extends EmptyInterceptor {

    private static final List<String> TENANT_FILTER_NAMES = List.of(
            "tenantFilter_Order",
            "tenantFilter_StorefrontConfig",
            "tenantFilter_EntityEvent",
            "tenantFilter_OutboxEvent",
            "tenantFilter_ApiKey",
            "tenantFilter_IdempotencyKey",
            "tenantFilter_Product",
            "tenantFilter_Inventory",
            "tenantFilter_Vendor");

    private static final Field TRANSACTION_SESSION_FIELD;

    static {
        Field f = null;
        try {
            f = Class.forName("org.hibernate.engine.transaction.internal.TransactionImpl")
                    .getDeclaredField("session");
            f.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            try {
                f = Class.forName("org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransaction")
                        .getDeclaredField("session");
                f.setAccessible(true);
            } catch (ReflectiveOperationException ex) {
                f = null;
            }
        }
        TRANSACTION_SESSION_FIELD = f;
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        Session session = resolveSession(tx);
        if (session == null) {
            return;
        }
        for (String filterName : TENANT_FILTER_NAMES) {
            if (session.getEnabledFilter(filterName) == null) {
                session.enableFilter(filterName).setParameter("tenantId", tenantId);
            }
        }
    }

    private Session resolveSession(Transaction tx) {
        if (TRANSACTION_SESSION_FIELD == null) {
            return null;
        }
        try {
            Object session = TRANSACTION_SESSION_FIELD.get(tx);
            return session instanceof Session ? (Session) session : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
