package com.vantage.order.app.event;

import java.util.UUID;

public record OrderCreatedPayload(UUID orderId, UUID productId, Integer quantity) {
}
