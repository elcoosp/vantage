package com.vantage.storefront.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "storefront_configs")
public class StorefrontConfig extends BaseTenantEntity {

    @Column(columnDefinition = "jsonb", nullable = false)
    private String layoutPayload;

    public String getLayoutPayload() {
        return layoutPayload;
    }

    public void setLayoutPayload(String layoutPayload) {
        this.layoutPayload = layoutPayload;
    }
}
