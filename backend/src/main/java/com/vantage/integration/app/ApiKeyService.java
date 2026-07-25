package com.vantage.integration.app;

import com.vantage.core.exception.ResourceNotFoundException;
import com.vantage.core.tenant.TenantContext;
import com.vantage.integration.domain.ApiKey;
import com.vantage.integration.domain.ApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX_VNT = "vnt_live_";

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiKeyWithPlain generateApiKey(String name) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        String plainKey = KEY_PREFIX_VNT + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String keyHash = passwordEncoder.encode(plainKey);
        String keyPrefix = plainKey.substring(0, 12);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setName(name);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setRevoked(false);

        apiKeyRepository.save(apiKey);

        return new ApiKeyWithPlain(apiKey, plainKey);
    }

    @Transactional
    public void revokeApiKey(UUID keyId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));
        if (!apiKey.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("API key not found for this tenant");
        }
        apiKey.setRevoked(true);
        apiKeyRepository.save(apiKey);
    }

    public record ApiKeyWithPlain(ApiKey apiKey, String plainKey) {}
}
