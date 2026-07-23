package com.vantage.inventory.app;

import com.vantage.core.exception.VantageDomainException;

public class InventoryConflictException extends VantageDomainException {
    public InventoryConflictException(String message) {
        super(message);
    }
}
