package com.vantage.analytics.messaging;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class OrderCreatedEvent extends ApplicationEvent {
    private final UUID orderId;
    private final UUID productId;
    private final UUID tenantId;

    public OrderCreatedEvent(Object source, UUID orderId, UUID productId, UUID tenantId) {
        super(source);
        this.orderId = orderId;
        this.productId = productId;
        this.tenantId = tenantId;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public UUID getTenantId() { return tenantId; }
}
