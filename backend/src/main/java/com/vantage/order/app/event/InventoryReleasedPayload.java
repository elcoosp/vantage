package com.vantage.order.app.event;

import java.util.UUID;

public record InventoryReleasedPayload(UUID orderId, UUID tenantId, UUID productId, Integer releasedQuantity) {
}
