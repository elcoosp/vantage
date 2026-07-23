package com.vantage.core.exception;

public abstract class VantageDomainException extends RuntimeException {
    protected VantageDomainException(String message) {
        super(message);
    }
}
