package com.vantage.storefront.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filter;
import java.util.UUID;

@Entity
@FilterDef(name = "tenantFilter_StorefrontConfig", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_StorefrontConfig", condition = "tenant_id = :tenantId")
@Table(name = "storefront_configs")
public class StorefrontConfig extends BaseTenantEntity {

    @Column(columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String layoutPayload;

    public String getLayoutPayload() {
        return layoutPayload;
    }

    public void setLayoutPayload(String layoutPayload) {
        this.layoutPayload = layoutPayload;
    }
}
