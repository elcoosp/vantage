package com.vantage.payment.ui.dto;

import java.util.UUID;

public record PaymentResponse(UUID transactionId, String status, String message) {
}
