package com.vantage.core.websocket.dto;

/**
 * Payload broadcast on {@code /topic/ops-map.<tenantId>} whenever an order
 * advances through the fulfillment lifecycle.
 *
 * <p>Coordinates are privacy-preserving fulfillment estimates derived
 * deterministically from the order id (see {@code OrderGeoEstimator}); they do
 * not encode any customer address.</p>
 */
public record OpsMapPinPayload(
        String orderId,
        double lat,
        double lon,
        String productName,
        String status,
        String timestamp
) {
}
