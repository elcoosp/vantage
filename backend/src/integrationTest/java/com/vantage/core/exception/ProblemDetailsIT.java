package com.vantage.core.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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
@Import(ProblemDetailsIT.TestSecurityConfig.class)
@Testcontainers
public class ProblemDetailsIT {

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

    @Test
    void should_return_problem_details_with_custom_properties_when_inventory_conflict_occurs() throws Exception {
        // Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "problem-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        // Create product
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Problem Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // Set initial inventory version 0
        HttpHeaders updateHeaders = new HttpHeaders();
        updateHeaders.setBearerAuth(token);
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        updateHeaders.set("X-Tenant-ID", tenantId.toString());
        updateHeaders.setIfMatch("0");
        InventoryUpdateRequest initReq = new InventoryUpdateRequest(10);
        HttpEntity<InventoryUpdateRequest> initEntity = new HttpEntity<>(initReq, updateHeaders);
        ResponseEntity<InventoryResponse> initRes = restTemplate.exchange(
                "/api/v1/inventory/" + productId, HttpMethod.PUT, initEntity, InventoryResponse.class);
        assertThat(initRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long currentVersion = initRes.getBody().version();

        // Trigger conflict: send update with stale version (currentVersion - 1)
        Long staleVersion = currentVersion - 1;
        HttpHeaders conflictHeaders = new HttpHeaders();
        conflictHeaders.setBearerAuth(token);
        conflictHeaders.setContentType(MediaType.APPLICATION_JSON);
        conflictHeaders.set("X-Tenant-ID", tenantId.toString());
        conflictHeaders.setIfMatch(String.valueOf(staleVersion));
        InventoryUpdateRequest conflictReq = new InventoryUpdateRequest(20);
        HttpEntity<InventoryUpdateRequest> conflictEntity = new HttpEntity<>(conflictReq, conflictHeaders);

        ResponseEntity<String> conflictResponse = restTemplate.exchange(
                "/api/v1/inventory/" + productId, HttpMethod.PUT, conflictEntity, String.class);

        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("application/problem+json"));

        JsonNode problemJson = objectMapper.readTree(conflictResponse.getBody());

        assertThat(problemJson.has("type")).isTrue();
        assertThat(problemJson.get("type").asText()).isEqualTo("https://vantage.io/errors/inventory-conflict");
        assertThat(problemJson.has("title")).isTrue();
        assertThat(problemJson.get("title").asText()).isEqualTo("Inventory Conflict");
        assertThat(problemJson.has("status")).isTrue();
        assertThat(problemJson.get("status").asInt()).isEqualTo(409);
        assertThat(problemJson.has("detail")).isTrue();
        assertThat(problemJson.get("detail").asText()).contains("Version mismatch");
        assertThat(problemJson.has("instance")).isTrue();
        assertThat(problemJson.get("instance").asText()).startsWith("/api/v1/inventory/");

        // Custom properties
        assertThat(problemJson.has("currentVersion")).isTrue();
        assertThat(problemJson.get("currentVersion").asLong()).isEqualTo(currentVersion);
        assertThat(problemJson.has("expectedVersion")).isTrue();
        assertThat(problemJson.get("expectedVersion").asLong()).isEqualTo(staleVersion);
    }
}
