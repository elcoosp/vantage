package com.vantage.storefront;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StorefrontControllerIT.TestSecurityConfig.class)
@Testcontainers
public class StorefrontControllerIT {

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

    private UUID registerVendor() {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "storefront-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
                "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return vendorRes.getBody().tenantId();
    }

    @Test
    void should_return_empty_layout_when_no_config_exists() throws Exception {
        UUID tenantId = registerVendor();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("dummy-token"); // security disabled, but header may be required
        headers.set("X-Tenant-ID", tenantId.toString());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/storefront",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isEqualTo(0);
    }

    @Test
    void should_update_layout_and_return_updated() throws Exception {
        UUID tenantId = registerVendor();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("dummy-token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        // Updated layout as JSON array
        List<Map<String, Object>> layout = List.of(
                Map.of("componentType", "HeroBanner", "props", Map.of("title", "Welcome", "imageUrl", "http://example.com/img.jpg")),
                Map.of("componentType", "ProductGrid", "props", Map.of("title", "Featured", "limit", 4))
        );
        String payload = objectMapper.writeValueAsString(layout);

        HttpEntity<String> putEntity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> putResponse = restTemplate.exchange(
                "/api/v1/storefront",
                HttpMethod.PUT,
                putEntity,
                String.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // We expect the returned body to be the updated layout? Or maybe just a success response.
        // The spec doesn't specify response body for PUT, but we can return the updated layout.
        // For now, we'll check that it returns the same array.
        JsonNode putJson = objectMapper.readTree(putResponse.getBody());
        assertThat(putJson.isArray()).isTrue();
        assertThat(putJson.size()).isEqualTo(2);

        // GET to verify persistence
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/v1/storefront",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode getJson = objectMapper.readTree(getResponse.getBody());
        assertThat(getJson).isEqualTo(putJson);
    }
}
