package com.vantage.core.events;

import java.util.UUID;

public record PaymentSucceededPayload(UUID orderId, UUID tenantId) {
}
