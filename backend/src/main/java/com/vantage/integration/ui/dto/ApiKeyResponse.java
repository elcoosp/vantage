package com.vantage.integration.ui.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String name,
    String key,
    String prefix,
    Instant createdAt
) {
    public static ApiKeyResponse withPlainKey(com.vantage.integration.domain.ApiKey apiKey, String plainKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            apiKey.getName(),
            plainKey,
            apiKey.getKeyPrefix(),
            apiKey.getCreatedAt()
        );
    }

    public static ApiKeyResponse withoutPlainKey(com.vantage.integration.domain.ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            apiKey.getName(),
            null,
            apiKey.getKeyPrefix(),
            apiKey.getCreatedAt()
        );
    }
}
