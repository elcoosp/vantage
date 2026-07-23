// backend/src/main/java/com/vantage/order/app/event/OrderCreatedPayload.java
package com.vantage.order.app.event;

import java.util.UUID;

public record OrderCreatedPayload(UUID orderId, UUID tenantId, UUID productId, Integer quantity) {
}
