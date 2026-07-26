package com.vantage.core.admin;

import com.vantage.core.admin.ui.dto.ChaosMonkeyToggleRequest;
import com.vantage.core.admin.ui.dto.SystemMetricsResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
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
@Import(AdminControllerIT.TestSecurityConfig.class)
@Testcontainers
public class AdminControllerIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_toggle_payment_failure_and_return_status() {
        ResponseEntity<Boolean> getResponse = restTemplate.getForEntity("/api/v1/admin/chaos-monkey/payment-failure", Boolean.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isFalse();

        ChaosMonkeyToggleRequest enableRequest = new ChaosMonkeyToggleRequest(true);
        HttpEntity<ChaosMonkeyToggleRequest> enableEntity = new HttpEntity<>(enableRequest);
        ResponseEntity<Void> enableResponse = restTemplate.postForEntity("/api/v1/admin/chaos-monkey/payment-failure", enableEntity, Void.class);
        assertThat(enableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        getResponse = restTemplate.getForEntity("/api/v1/admin/chaos-monkey/payment-failure", Boolean.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isTrue();

        ChaosMonkeyToggleRequest disableRequest = new ChaosMonkeyToggleRequest(false);
        HttpEntity<ChaosMonkeyToggleRequest> disableEntity = new HttpEntity<>(disableRequest);
        ResponseEntity<Void> disableResponse = restTemplate.postForEntity("/api/v1/admin/chaos-monkey/payment-failure", disableEntity, Void.class);
        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        getResponse = restTemplate.getForEntity("/api/v1/admin/chaos-monkey/payment-failure", Boolean.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isFalse();
    }

    @Test
    void should_return_system_metrics() {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "metrics-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        HttpHeaders invHeaders = new HttpHeaders();
        invHeaders.setBearerAuth(token);
        invHeaders.setContentType(MediaType.APPLICATION_JSON);
        invHeaders.set("X-Tenant-ID", tenantId.toString());
        invHeaders.setIfMatch("0");
        com.vantage.inventory.ui.dto.InventoryUpdateRequest invReq = new com.vantage.inventory.ui.dto.InventoryUpdateRequest(10);
        HttpEntity<com.vantage.inventory.ui.dto.InventoryUpdateRequest> invEntity = new HttpEntity<>(invReq, invHeaders);
        ResponseEntity<com.vantage.inventory.ui.dto.InventoryResponse> invRes = restTemplate.exchange(
            "/api/v1/inventory/" + productId, HttpMethod.PUT, invEntity, com.vantage.inventory.ui.dto.InventoryResponse.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderRequest orderReq = new OrderRequest(productId, 2, "Test Product");
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, authHeaders);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity("/api/v1/orders", orderEntity, OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<SystemMetricsResponse> metricsResponse = restTemplate.getForEntity("/api/v1/admin/metrics", SystemMetricsResponse.class);
        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SystemMetricsResponse metrics = metricsResponse.getBody();
        assertThat(metrics).isNotNull();
        assertThat(metrics.totalVendors()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.totalOrders()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.paymentCircuitBreakerState()).isNotBlank();
    }
}
