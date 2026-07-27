// backend/src/main/java/com/vantage/order/app/event/OrderCreatedPayload.java
package com.vantage.core.events;

import java.util.UUID;

public record OrderCreatedPayload(UUID orderId, UUID tenantId, UUID productId, String productName, Integer quantity) {
}
