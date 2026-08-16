package com.vantage.product.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@FilterDef(name = "tenantFilter_Product", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_Product", condition = "tenant_id = :tenantId")
@Table(name = "products")
public class Product extends BaseTenantEntity {
    private String name;
    private String description;
    private BigDecimal price;
private String sku;
public String getSku() { return sku; }
public void setSku(String sku) { this.sku = sku; }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
