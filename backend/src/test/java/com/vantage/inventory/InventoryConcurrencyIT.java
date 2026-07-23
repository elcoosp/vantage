package com.vantage.inventory;

import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InventoryConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void should_return_409_conflict_when_concurrent_inventory_updates_use_same_version() throws Exception {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest("test@vantage.com", "securePassword123", "Vantage Inc.");
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorReq, AuthResponse.class);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        ProductRequest productReq = new ProductRequest("Test Product", "Description", 100.0);
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        UUID productId = productRes.getBody().id();

        Thread.sleep(1000);

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
