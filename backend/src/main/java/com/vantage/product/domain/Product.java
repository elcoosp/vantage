package com.vantage.product.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseTenantEntity {
    private String name;
    private String description;
    private BigDecimal price;
}
