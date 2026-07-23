package com.vantage.core.tenant;

public class MissingTenantContextException extends RuntimeException {
    public MissingTenantContextException(String message) {
        super(message);
    }
}
