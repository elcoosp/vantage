package com.vantage.inventory.app.event;

import java.util.UUID;

public record InventoryReservedPayload(UUID orderId, UUID tenantId, UUID productId, Integer reservedQuantity) {
}
