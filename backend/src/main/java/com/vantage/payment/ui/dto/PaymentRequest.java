package com.vantage.payment.ui.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    @NotNull UUID orderId,
    @NotNull @Positive BigDecimal amount,
    @NotNull String currency
) {
}
