package com.vantage.storefront.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.tenant.TenantContext;
import com.vantage.storefront.domain.StorefrontConfig;
import com.vantage.storefront.domain.StorefrontRepository;
import com.vantage.storefront.ui.dto.StorefrontLayoutResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storefront")
public class StorefrontController {

    private final StorefrontRepository storefrontRepository;
    private final ObjectMapper objectMapper;

    public StorefrontController(StorefrontRepository storefrontRepository, ObjectMapper objectMapper) {
        this.storefrontRepository = storefrontRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<StorefrontLayoutResponse> getLayout() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return storefrontRepository.findByTenantId(tenantId)
                .map(config -> {
                    try {
                        List<Map<String, Object>> components = objectMapper.readValue(
                                config.getLayoutPayload(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );
                        return ResponseEntity.ok(new StorefrontLayoutResponse(components));
                    } catch (JsonProcessingException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElseGet(() -> ResponseEntity.ok(new StorefrontLayoutResponse(new ArrayList<>())));
    }

    @PutMapping
    public ResponseEntity<StorefrontLayoutResponse> updateLayout(@RequestBody List<Map<String, Object>> components) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(components);

            StorefrontConfig config = storefrontRepository.findByTenantId(tenantId)
                    .orElseGet(() -> {
                        StorefrontConfig newConfig = new StorefrontConfig();
                        newConfig.setTenantId(tenantId);
                        return newConfig;
                    });

            config.setLayoutPayload(jsonPayload);
            storefrontRepository.save(config);

            return ResponseEntity.ok(new StorefrontLayoutResponse(components));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
