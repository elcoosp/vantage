package com.vantage.analytics.messaging;

import java.util.UUID;

public class OrderCreatedEvent {
    private final UUID orderId;
    private final UUID productId;
    private final UUID tenantId;

    public OrderCreatedEvent(UUID orderId, UUID productId, UUID tenantId) {
        this.orderId = orderId;
        this.productId = productId;
        this.tenantId = tenantId;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public UUID getTenantId() { return tenantId; }
}
