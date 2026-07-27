package com.vantage.core.events;

import java.util.UUID;

public record InventoryReleasedPayload(UUID orderId, UUID tenantId, UUID productId, Integer releasedQuantity) {
}
