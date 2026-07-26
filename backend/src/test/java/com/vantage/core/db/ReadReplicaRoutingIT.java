package com.vantage.core.db;

import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReadReplicaRoutingIT.TestSecurityConfig.class)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class ReadReplicaRoutingIT {

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
    static PostgreSQLContainer<?> primaryPostgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vantage_primary")
            .withUsername("vantage")
            .withPassword("vantage_pw");

    @Container
    static PostgreSQLContainer<?> replicaPostgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vantage_replica")
            .withUsername("vantage")
            .withPassword("vantage_pw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.url", primaryPostgres::getJdbcUrl);
        registry.add("spring.datasource.primary.username", primaryPostgres::getUsername);
        registry.add("spring.datasource.primary.password", primaryPostgres::getPassword);
        registry.add("spring.datasource.replica.url", replicaPostgres::getJdbcUrl);
        registry.add("spring.datasource.replica.username", replicaPostgres::getUsername);
        registry.add("spring.datasource.replica.password", replicaPostgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("vantage.outbox.enabled", () -> "false");
        registry.add("vantage.inventory.consumer.enabled", () -> "false");
        registry.add("vantage.payment.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;
    private UUID tenantId;

    @BeforeEach
    void setup() {
        // Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "routing-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        token = vendorRes.getBody().token();
        tenantId = vendorRes.getBody().tenantId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UUID createProduct(String name, String description, BigDecimal price) {
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest(name, description, price);
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        return productRes.getBody().id();
    }

    private void setInventory(UUID productId, int quantity, long version) {
        HttpHeaders invHeaders = new HttpHeaders();
        invHeaders.setBearerAuth(token);
        invHeaders.setContentType(MediaType.APPLICATION_JSON);
        invHeaders.set("X-Tenant-ID", tenantId.toString());
        invHeaders.setIfMatch(String.valueOf(version));
        InventoryUpdateRequest invReq = new InventoryUpdateRequest(quantity);
        HttpEntity<InventoryUpdateRequest> invEntity = new HttpEntity<>(invReq, invHeaders);
        ResponseEntity<InventoryResponse> invRes = restTemplate.exchange(
                "/api/v1/inventory/" + productId, HttpMethod.PUT, invEntity, InventoryResponse.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_use_replica_for_read_only_transaction(CapturedOutput output) {
        UUID productId = createProduct("Routing Test Product", "Description", new BigDecimal("99.99"));

        // Perform read-only GET request
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.setBearerAuth(token);
        getHeaders.set("X-Tenant-ID", tenantId.toString());

        ResponseEntity<ProductResponse> getRes = restTemplate.exchange(
                "/api/v1/products/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders),
                ProductResponse.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify log contains routing to REPLICA
        assertThat(output).contains("ReplicaRoutingInterceptor setting context to: REPLICA");
        assertThat(output).contains("Routing datasource: REPLICA");
    }

    @Test
    void should_use_primary_for_write_transaction(CapturedOutput output) {
        UUID productId = createProduct("Write Test Product", "Description", new BigDecimal("99.99"));
        setInventory(productId, 10, 0);

        // Perform write operation (POST /orders)
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Tenant-ID", tenantId.toString());

        OrderRequest orderReq = new OrderRequest(productId, 2, "Write Test Product");
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, authHeaders);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
                "/api/v1/orders", orderEntity, OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(orderRes.getBody()).isNotNull();

        // Verify log contains routing to PRIMARY
        assertThat(output).contains("ReplicaRoutingInterceptor setting context to: PRIMARY");
        assertThat(output).contains("Routing datasource: PRIMARY");
    }
}
