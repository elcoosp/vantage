package com.vantage.integration;

import com.vantage.core.tenant.TenantFilter;
import com.vantage.integration.domain.ApiKey;
import com.vantage.integration.domain.ApiKeyRepository;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Import(ApiKeyAuthenticationIT.TestConfig.class)
@Testcontainers
public class ApiKeyAuthenticationIT {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TenantFilter tenantFilter() {
            return new TenantFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                        throws ServletException, IOException {
                    // Skip tenant header check; pass through
                    filterChain.doFilter(request, response);
                }
            };
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

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void should_authenticate_with_valid_api_key_and_return_200() {
        // Register a vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "api-test-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID tenantId = vendorRes.getBody().tenantId();

        // Create an API key directly in the repository
        String plainKey = "vnt_live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String keyHash = passwordEncoder.encode(plainKey);
        String keyPrefix = plainKey.substring(0, 12);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setName("Test Key");
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setRevoked(false);
        apiKeyRepository.save(apiKey);

        // Attempt to create a product using the API key
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", plainKey);

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);

        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);

        System.out.println("Product creation response status: " + response.getStatusCode());
        System.out.println("Product creation response body: " + response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Product");
    }

    @Test
    void should_return_401_when_no_api_key_provided() {
        ProductRequest productReq = new ProductRequest("Unauth Product", "Should fail", new BigDecimal("10.00"));
        HttpEntity<ProductRequest> entity = new HttpEntity<>(productReq, new HttpHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/products", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void should_return_401_when_api_key_is_invalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", "invalid_key");
        ProductRequest productReq = new ProductRequest("Invalid Key", "Should fail", new BigDecimal("10.00"));
        HttpEntity<ProductRequest> entity = new HttpEntity<>(productReq, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/products", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void should_return_401_when_api_key_is_revoked() {
        // Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "revoke-test-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID tenantId = vendorRes.getBody().tenantId();

        // Create a revoked key
        String plainKey = "vnt_live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String keyHash = passwordEncoder.encode(plainKey);
        String keyPrefix = plainKey.substring(0, 12);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setName("Revoked Key");
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setRevoked(true);
        apiKeyRepository.save(apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", plainKey);
        ProductRequest productReq = new ProductRequest("Revoked", "Should fail", new BigDecimal("10.00"));
        HttpEntity<ProductRequest> entity = new HttpEntity<>(productReq, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/products", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
