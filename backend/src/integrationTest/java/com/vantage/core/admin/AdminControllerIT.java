package com.vantage.core.admin;

import com.vantage.VantageApplication;
import org.springframework.context.annotation.Import;
import com.vantage.AbstractIntegrationTest;

import com.vantage.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.admin.ui.dto.SystemMetricsResponse;
import tools.jackson.databind.ObjectMapper;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.inventory.ui.dto.InventoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = VantageApplication.class)
@Import(AbstractIntegrationTest.TestRestClientConfig.class)
public class AdminControllerIT  extends AbstractIntegrationTest {
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
    }

    @Autowired
    private RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private UUID adminTenantId;

    @BeforeEach
    void registerVendorAndGetToken() {
        // Register a vendor to get a valid JWT token
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "admin-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Admin Vendor");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        ResponseEntity<AuthResponse> vendorRes = restClient.post()
                .uri("/api/v1/vendors/register")
                .headers(h -> h.addAll(vendorHeaders))
                .body(vendorReq)
                .retrieve()
                .toEntity(AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        adminToken = vendorRes.getBody().token();
        adminTenantId = vendorRes.getBody().tenantId();
    }

    @Test
    void should_toggle_payment_failure_and_return_status() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", adminTenantId.toString());

        // Reset state: disable chaos monkey
        ChaosMonkeyToggleRequest disableRequest = new ChaosMonkeyToggleRequest(false);
        ResponseEntity<Void> disableReset = restClient.post()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .body(disableRequest)
                .retrieve()
                .toEntity(Void.class);
        assertThat(disableReset.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET initial status
        ResponseEntity<String> getRaw = restClient.get()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
        System.out.println("GET initial status: code=" + getRaw.getStatusCode() + ", body=" + getRaw.getBody());
        assertThat(getRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean initial = objectMapper.readValue(getRaw.getBody(), Boolean.class);
        assertThat(initial).isFalse();

        // Enable chaos monkey
        ChaosMonkeyToggleRequest enableRequest = new ChaosMonkeyToggleRequest(true);
        ResponseEntity<Void> enableRaw = restClient.post()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .body(enableRequest)
                .retrieve()
                .toEntity(Void.class);
        System.out.println("Enable POST: code=" + enableRaw.getStatusCode());
        assertThat(enableRaw.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET status after enable
        getRaw = restClient.get()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
        System.out.println("GET after enable: code=" + getRaw.getStatusCode() + ", body=" + getRaw.getBody());
        assertThat(getRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean enabled = objectMapper.readValue(getRaw.getBody(), Boolean.class);
        assertThat(enabled).isTrue();

        // Disable chaos monkey
        ResponseEntity<Void> disableRaw = restClient.post()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .body(new ChaosMonkeyToggleRequest(false))
                .retrieve()
                .toEntity(Void.class);
        System.out.println("Disable POST: code=" + disableRaw.getStatusCode());
        assertThat(disableRaw.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET status after disable
        getRaw = restClient.get()
                .uri("/api/v1/admin/chaos-monkey/payment-failure")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
        System.out.println("GET after disable: code=" + getRaw.getStatusCode() + ", body=" + getRaw.getBody());
        assertThat(getRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean disabled = objectMapper.readValue(getRaw.getBody(), Boolean.class);
        assertThat(disabled).isFalse();
    }

    @Test
    void should_return_system_metrics() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", adminTenantId.toString());

        // Create a product
        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        ResponseEntity<ProductResponse> productRes = restClient.post()
                .uri("/api/v1/products")
                .headers(h -> h.addAll(headers))
                .body(productReq)
                .retrieve()
                .toEntity(ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // Set inventory
        HttpHeaders invHeaders = new HttpHeaders();
        invHeaders.setBearerAuth(adminToken);
        invHeaders.setContentType(MediaType.APPLICATION_JSON);
        invHeaders.set("X-Tenant-ID", adminTenantId.toString());
        invHeaders.setIfMatch("0");
        InventoryUpdateRequest invReq = new InventoryUpdateRequest(10);
        ResponseEntity<InventoryResponse> invRes = restClient.put()
                .uri("/api/v1/inventory/" + productId)
                .headers(h -> h.addAll(invHeaders))
                .body(invReq)
                .retrieve()
                .toEntity(InventoryResponse.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Create order
        OrderRequest orderReq = new OrderRequest(productId, 2, "Test Product");
        ResponseEntity<OrderResponse> orderRes = restClient.post()
                .uri("/api/v1/orders")
                .headers(h -> h.addAll(headers))
                .body(orderReq)
                .retrieve()
                .toEntity(OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Get metrics
        ResponseEntity<String> metricsRaw = restClient.get()
                .uri("/api/v1/admin/metrics")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
        System.out.println("Metrics response: code=" + metricsRaw.getStatusCode() + ", body=" + metricsRaw.getBody());
        assertThat(metricsRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        SystemMetricsResponse metrics = objectMapper.readValue(metricsRaw.getBody(), SystemMetricsResponse.class);
        assertThat(metrics).isNotNull();
        assertThat(metrics.totalVendors()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.totalOrders()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.paymentCircuitBreakerState()).isIn("CLOSED", "UNKNOWN");
    }
}