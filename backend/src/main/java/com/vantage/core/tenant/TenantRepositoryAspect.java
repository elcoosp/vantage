package com.vantage.core.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TenantRepositoryAspect {

    private final EntityManager entityManager;

    public TenantRepositoryAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("target(org.springframework.data.repository.Repository+)")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
