package com.vantage.payment.app.event;

import java.util.UUID;

public record PaymentFailedPayload(UUID orderId, UUID tenantId, String reason) {
}
