package com.vantage.admin.ui.dto;

public record SystemMetricsResponse(long totalVendors, long totalOrders, String paymentCircuitBreakerState) {
}
