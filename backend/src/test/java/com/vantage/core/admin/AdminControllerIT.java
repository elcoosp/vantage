package com.vantage.core.admin;

import com.vantage.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.admin.ui.dto.SystemMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AdminControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.primary.username", postgres::getUsername);
        registry.add("spring.datasource.primary.password", postgres::getPassword);
        registry.add("spring.datasource.replica.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.replica.username", postgres::getUsername);
        registry.add("spring.datasource.replica.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

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
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
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
        HttpEntity<ChaosMonkeyToggleRequest> disableEntity = new HttpEntity<>(disableRequest, headers);
        ResponseEntity<Void> disableReset = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.POST,
            disableEntity,
            Void.class
        );
        assertThat(disableReset.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET initial status
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<String> getRaw = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.GET,
            getEntity,
            String.class
        );
        System.out.println("GET initial status: code=" + getRaw.getStatusCode() + ", body=" + getRaw.getBody());
        assertThat(getRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean initial = objectMapper.readValue(getRaw.getBody(), Boolean.class);
        assertThat(initial).isFalse();

        // Enable chaos monkey
        ChaosMonkeyToggleRequest enableRequest = new ChaosMonkeyToggleRequest(true);
        HttpEntity<ChaosMonkeyToggleRequest> enableEntity = new HttpEntity<>(enableRequest, headers);
        ResponseEntity<Void> enableRaw = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.POST,
            enableEntity,
            Void.class
        );
        System.out.println("Enable POST: code=" + enableRaw.getStatusCode());
        assertThat(enableRaw.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET status after enable
        getRaw = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.GET,
            getEntity,
            String.class
        );
        System.out.println("GET after enable: code=" + getRaw.getStatusCode() + ", body=" + getRaw.getBody());
        assertThat(getRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean enabled = objectMapper.readValue(getRaw.getBody(), Boolean.class);
        assertThat(enabled).isTrue();

        // Disable chaos monkey
        disableEntity = new HttpEntity<>(new ChaosMonkeyToggleRequest(false), headers);
        ResponseEntity<Void> disableRaw = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.POST,
            disableEntity,
            Void.class
        );
        System.out.println("Disable POST: code=" + disableRaw.getStatusCode());
        assertThat(disableRaw.getStatusCode()).isEqualTo(HttpStatus.OK);

        // GET status after disable
        getRaw = restTemplate.exchange(
            "/api/v1/admin/chaos-monkey/payment-failure",
            HttpMethod.GET,
            getEntity,
            String.class
        );
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
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // Set inventory
        HttpHeaders invHeaders = new HttpHeaders();
        invHeaders.setBearerAuth(adminToken);
        invHeaders.setContentType(MediaType.APPLICATION_JSON);
        invHeaders.set("X-Tenant-ID", adminTenantId.toString());
        invHeaders.setIfMatch("0");
        InventoryUpdateRequest invReq = new InventoryUpdateRequest(10);
        HttpEntity<InventoryUpdateRequest> invEntity = new HttpEntity<>(invReq, invHeaders);
        ResponseEntity<InventoryResponse> invRes = restTemplate.exchange(
            "/api/v1/inventory/" + productId, HttpMethod.PUT, invEntity, InventoryResponse.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Create order
        OrderRequest orderReq = new OrderRequest(productId, 2, "Test Product");
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, headers);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity("/api/v1/orders", orderEntity, OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Get metrics
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<String> metricsRaw = restTemplate.exchange(
            "/api/v1/admin/metrics",
            HttpMethod.GET,
            getEntity,
            String.class
        );
        System.out.println("Metrics response: code=" + metricsRaw.getStatusCode() + ", body=" + metricsRaw.getBody());
        assertThat(metricsRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        SystemMetricsResponse metrics = objectMapper.readValue(metricsRaw.getBody(), SystemMetricsResponse.class);
        assertThat(metrics).isNotNull();
        assertThat(metrics.totalVendors()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.totalOrders()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.paymentCircuitBreakerState()).isIn("CLOSED", "UNKNOWN");
    }
}
