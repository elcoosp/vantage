package com.vantage.core.audit.ui.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String payload,
    Instant createdAt
) {
}
