package com.vantage.core.events;

import java.util.UUID;

public record InventoryReservationFailedPayload(UUID orderId, UUID tenantId, UUID productId, Integer failedQuantity) {
}
