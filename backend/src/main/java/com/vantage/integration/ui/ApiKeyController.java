package com.vantage.integration.ui;

import com.vantage.integration.app.ApiKeyService;
import com.vantage.integration.domain.ApiKey;
import com.vantage.integration.domain.ApiKeyRepository;
import com.vantage.integration.ui.dto.ApiKeyResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyController(ApiKeyService apiKeyService, ApiKeyRepository apiKeyRepository) {
        this.apiKeyService = apiKeyService;
        this.apiKeyRepository = apiKeyRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyResponse generateApiKey(@RequestBody GenerateRequest request) {
        ApiKeyService.ApiKeyWithPlain result = apiKeyService.generateApiKey(request.name());
        return ApiKeyResponse.withPlainKey(result.apiKey(), result.plainKey());
    }

    @GetMapping
    public List<ApiKeyResponse> listApiKeys() {
        UUID tenantId = com.vantage.core.tenant.TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        return apiKeyRepository.findByTenantIdAndRevokedFalse(tenantId)
                .stream()
                .map(ApiKeyResponse::withoutPlainKey)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(@PathVariable UUID id) {
        apiKeyService.revokeApiKey(id);
    }

    public record GenerateRequest(@NotBlank String name) {}
}
