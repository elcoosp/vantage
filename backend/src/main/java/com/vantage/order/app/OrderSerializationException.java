package com.vantage.order.app;

import com.vantage.core.exception.VantageDomainException;

public class OrderSerializationException extends VantageDomainException {
    public OrderSerializationException(String message) {
        super(message);
    }
}
