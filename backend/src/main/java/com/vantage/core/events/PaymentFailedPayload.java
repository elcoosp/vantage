package com.vantage.core.events;

import java.util.UUID;

public record PaymentFailedPayload(UUID orderId, UUID tenantId, String reason) {
}
