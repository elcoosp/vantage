package com.vantage.core.events;

import java.util.UUID;

/**
 * Payload published to the outbox (and mirrored on the in-process event bus)
 * whenever an order transitions through the fulfillment lifecycle, e.g.
 * PAID → SHIPPED or SHIPPED → DELIVERED.
 *
 * <p>Consumed by the order search projector (query side) and by the live ops
 * map publisher (websocket module) via the {@code vantage.events} exchange
 * with routing keys {@code OrderShippedEvent} / {@code OrderDeliveredEvent}.</p>
 */
public record OrderStatusChangedPayload(
        UUID orderId,
        UUID tenantId,
        String oldStatus,
        String newStatus,
        String productName,
        Integer quantity
) {
}
