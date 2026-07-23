package com.vantage.order.ui.dto;

import com.vantage.order.domain.OrderStatus;

import java.util.UUID;

public record OrderResponse(UUID id, UUID productId, Integer quantity, OrderStatus status) {
}
