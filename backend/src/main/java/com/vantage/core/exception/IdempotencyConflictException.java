package com.vantage.core.exception;

public class IdempotencyConflictException extends VantageDomainException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
