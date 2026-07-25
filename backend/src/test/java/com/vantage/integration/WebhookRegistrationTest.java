package com.vantage.integration;

import com.vantage.integration.ui.dto.WebhookUpdateRequest;
import com.vantage.integration.ui.dto.WebhookUpdateResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class WebhookRegistrationTest {

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

    @Test
    void should_return_secret_and_update_vendor_when_updating_webhook() {
        // Register a vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "webhook-" + UUID.randomUUID() + "@vantage.com",
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

        System.out.println("Vendor registered with tenantId: " + tenantId);

        // Set TenantContext manually for the webhook update
        com.vantage.core.tenant.TenantContext.setTenantId(tenantId);
        try {
            // Update webhook URL
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId.toString());

            WebhookUpdateRequest request = new WebhookUpdateRequest("https://example.com/webhook");
            HttpEntity<WebhookUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<WebhookUpdateResponse> response = restTemplate.exchange(
                "/api/v1/webhooks",
                HttpMethod.PUT,
                entity,
                WebhookUpdateResponse.class);

            System.out.println("Webhook update response status: " + response.getStatusCode());
            System.out.println("Webhook update response body: " + response.getBody());
            System.out.println("Webhook update response headers: " + response.getHeaders());

            if (response.getStatusCode() != HttpStatus.OK && response.getBody() != null) {
                // If it's an error response, try to parse and print details
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.vantage.core.exception.ErrorResponse error = mapper.readValue(response.getBody().toString(), com.vantage.core.exception.ErrorResponse.class);
                    System.out.println("Error response: " + error);
                } catch (Exception e) {
                    System.out.println("Could not parse error response");
                }
            }

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new AssertionError("Expected 200 OK but got " + response.getStatusCode() + " with body: " + response.getBody());
            }
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().secret()).isNotBlank();
        } finally {
            com.vantage.core.tenant.TenantContext.clear();
        }
    }
}