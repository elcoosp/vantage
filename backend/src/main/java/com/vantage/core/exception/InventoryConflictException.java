package com.vantage.core.exception;

public class InventoryConflictException extends VantageDomainException {
    private final Long expectedVersion;
    private final Long currentVersion;

    public InventoryConflictException(String message, Long expectedVersion, Long currentVersion) {
        super(message);
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
