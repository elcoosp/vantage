package com.vantage.core.tenant;

import com.vantage.core.exception.VantageDomainException;

public class MissingTenantContextException extends VantageDomainException {
    public MissingTenantContextException(String message) {
        super(message);
    }
}
