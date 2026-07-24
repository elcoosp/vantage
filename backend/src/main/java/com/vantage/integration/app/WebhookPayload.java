package com.vantage.integration.app;

import java.time.Instant;
import java.util.UUID;

public record WebhookPayload(
    String eventType,
    UUID orderId,
    String status,
    Instant occurredAt
) {
}
