package com.vantage.payment.app.event;

import java.util.UUID;

public record PaymentSucceededPayload(UUID orderId, UUID tenantId) {
}
