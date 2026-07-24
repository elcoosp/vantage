package com.vantage.payment.app;

import com.vantage.core.exception.VantageDomainException;

public class IdempotencyConflictException extends VantageDomainException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
