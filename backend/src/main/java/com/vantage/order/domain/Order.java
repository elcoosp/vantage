package com.vantage.order.domain;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.UUID;

@Entity
@Table(name = "orders")

public class Order extends BaseTenantEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
@jakarta.persistence.JoinColumn(name = "product_id", insertable = false, updatable = false)
private com.vantage.product.domain.Product product;

public com.vantage.product.domain.Product getProduct() { return product; }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }


    @PrePersist
    public void prePersist() {
        com.vantage.core.audit.infrastructure.AuditHelper.captureInsert(this);
    }

    @PreUpdate
    public void preUpdate() {
        com.vantage.core.audit.infrastructure.AuditHelper.captureUpdate(this);
    }
}
