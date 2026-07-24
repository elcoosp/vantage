package com.vantage.inventory.app.event;

import java.util.UUID;

public record InventoryReservationFailedPayload(UUID orderId, UUID tenantId, UUID productId, Integer failedQuantity) {
}
