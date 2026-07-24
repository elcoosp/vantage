package com.vantage.payment.infrastructure;

import com.vantage.core.exception.VantageDomainException;

public class PaymentGatewayException extends VantageDomainException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}
