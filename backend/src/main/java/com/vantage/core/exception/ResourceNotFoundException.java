package com.vantage.core.exception;

public class ResourceNotFoundException extends VantageDomainException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
