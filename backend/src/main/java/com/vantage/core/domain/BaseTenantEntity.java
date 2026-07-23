package com.vantage.core.domain;

import com.vantage.core.tenant.MissingTenantContextException;
import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class BaseTenantEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new MissingTenantContextException("Tenant ID must be present in context for persistence");
            }
            this.tenantId = tenantId;
        }
    }
}
