package com.vantage.core.admin.ui.dto;

public record SystemMetricsResponse(long totalVendors, long totalOrders, String paymentCircuitBreakerState) {
}
