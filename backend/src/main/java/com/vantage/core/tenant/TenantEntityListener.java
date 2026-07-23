package com.vantage.core.tenant;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.UUID;

public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void setTenant(BaseTenantEntity entity) {
        if (entity.getTenantId() == null) {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new MissingTenantContextException("Tenant ID must be present in context for persistence");
            }
            entity.setTenantId(tenantId);
        }
    }
}
