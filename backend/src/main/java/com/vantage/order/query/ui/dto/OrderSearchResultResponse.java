package com.vantage.order.query.ui.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderSearchResultResponse(
    UUID orderId,
    String productName,
    String status,
    Integer quantity,
    Instant createdAt
) {
}
