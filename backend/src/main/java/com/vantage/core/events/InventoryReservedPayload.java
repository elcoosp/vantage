package com.vantage.core.events;

import java.util.UUID;

public record InventoryReservedPayload(UUID orderId, UUID tenantId, UUID productId, Integer reservedQuantity) {
}
