package com.vantage.product.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseTenantEntity {
    private String name;
    private String description;
    private BigDecimal price;
private String sku;
public String getSku() { return sku; }
public void setSku(String sku) { this.sku = sku; }

@jakarta.persistence.OneToMany(mappedBy = "product", fetch = jakarta.persistence.FetchType.LAZY)
private java.util.List<com.vantage.inventory.domain.Inventory> inventories = new java.util.ArrayList<>();
public java.util.List<com.vantage.inventory.domain.Inventory> getInventories() { return inventories; }

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
