package com.vantage.inventory;
import com.vantage.AbstractIntegrationTest;

import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryConcurrencyIT extends AbstractIntegrationTest {

    @TestConfiguration
    @Order(1)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(basic -> basic.disable());
            return http.build();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void should_return_409_conflict_when_concurrent_inventory_updates_use_same_version() throws Exception {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest("test@vantage.com", "securePassword123", "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", java.util.UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        UUID productId = productRes.getBody().id();

        HttpHeaders updateHeaders = new HttpHeaders();
        updateHeaders.setBearerAuth(token);
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        updateHeaders.setIfMatch("0");
        InventoryUpdateRequest initReq = new InventoryUpdateRequest(10);
        HttpEntity<InventoryUpdateRequest> initEntity = new HttpEntity<>(initReq, updateHeaders);
        ResponseEntity<InventoryResponse> initRes = restTemplate.exchange("/api/v1/inventory/" + productId, HttpMethod.PUT, initEntity, InventoryResponse.class);

        assertThat(initRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        long currentVersion = initRes.getBody().version();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Callable<ResponseEntity<InventoryResponse>> task1 = () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setIfMatch(String.valueOf(currentVersion));
            InventoryUpdateRequest req = new InventoryUpdateRequest(20);
            HttpEntity<InventoryUpdateRequest> entity = new HttpEntity<>(req, headers);
            return restTemplate.exchange("/api/v1/inventory/" + productId, HttpMethod.PUT, entity, InventoryResponse.class);
        };

        Callable<ResponseEntity<InventoryResponse>> task2 = () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setIfMatch(String.valueOf(currentVersion));
            InventoryUpdateRequest req = new InventoryUpdateRequest(30);
            HttpEntity<InventoryUpdateRequest> entity = new HttpEntity<>(req, headers);
            return restTemplate.exchange("/api/v1/inventory/" + productId, HttpMethod.PUT, entity, InventoryResponse.class);
        };

        Future<ResponseEntity<InventoryResponse>> future1 = executor.submit(task1);
        Future<ResponseEntity<InventoryResponse>> future2 = executor.submit(task2);

        ResponseEntity<InventoryResponse> response1 = future1.get();
        ResponseEntity<InventoryResponse> response2 = future2.get();

        boolean oneSucceeds = response1.getStatusCode() == HttpStatus.OK || response2.getStatusCode() == HttpStatus.OK;
        boolean oneConflicts = response1.getStatusCode() == HttpStatus.CONFLICT || response2.getStatusCode() == HttpStatus.CONFLICT;

        assertThat(oneSucceeds).isTrue();
        assertThat(oneConflicts).isTrue();

        TenantContext.setTenantId(tenantId);
        try {
            Inventory dbInventory = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(dbInventory.getVersion()).isEqualTo(currentVersion + 1);

            if (response1.getStatusCode() == HttpStatus.OK) {
                assertThat(dbInventory.getQuantity()).isEqualTo(20);
                assertThat(response1.getBody().version()).isEqualTo(currentVersion + 1);
            } else {
                assertThat(dbInventory.getQuantity()).isEqualTo(30);
                assertThat(response2.getBody().version()).isEqualTo(currentVersion + 1);
            }
        } finally {
            TenantContext.clear();
        }

        executor.shutdown();
    }
}
