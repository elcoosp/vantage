package com.vantage.payment;
import com.vantage.AbstractIntegrationTest;

import com.vantage.payment.ui.dto.PaymentRequest;
import com.vantage.payment.ui.dto.PaymentResponse;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IdempotentPaymentIT.TestSecurityConfig.class)
public class IdempotentPaymentIT  extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }



    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders createAuthHeaders(String token, UUID tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        return headers;
    }

    private AuthResponse registerVendorAndGetTenant() {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "payment-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return vendorRes.getBody();
    }

    @Test
    void should_return_200_and_transactionId_when_valid_request_with_new_key() {
        AuthResponse auth = registerVendorAndGetTenant();
        UUID tenantId = auth.tenantId();
        // Since we disabled security, we don't need token; but we still need to set header maybe not needed
        // We use the real JWT so TenantSecurityFilter authenticates the request.
        String token = auth.token();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.setBearerAuth(token);
        headers.set("Idempotency-Key", "key-1");

        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), new BigDecimal("500"), "USD");
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
            "/api/v1/payments", entity, PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().transactionId()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("SUCCESS");
    }

    @Test
    void should_return_same_transactionId_when_duplicate_request_with_same_key() {
        AuthResponse auth = registerVendorAndGetTenant();
        UUID tenantId = auth.tenantId();
        String token = auth.token();
        String idempotencyKey = "key-2";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.setBearerAuth(token);
        headers.set("Idempotency-Key", idempotencyKey);

        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), new BigDecimal("500"), "USD");
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentResponse> response1 = restTemplate.postForEntity(
            "/api/v1/payments", entity, PaymentResponse.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaymentResponse body1 = response1.getBody();

        // Duplicate
        ResponseEntity<PaymentResponse> response2 = restTemplate.postForEntity(
            "/api/v1/payments", entity, PaymentResponse.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaymentResponse body2 = response2.getBody();

        assertThat(body2.transactionId()).isEqualTo(body1.transactionId());
        assertThat(body2.status()).isEqualTo(body1.status());
        assertThat(body2.message()).isEqualTo(body1.message());
    }

    @Test
    void should_return_409_when_same_key_with_different_payload() {
        AuthResponse auth = registerVendorAndGetTenant();
        UUID tenantId = auth.tenantId();
        String token = auth.token();
        String idempotencyKey = "key-3";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        headers.setBearerAuth(token);
        headers.set("Idempotency-Key", idempotencyKey);

        PaymentRequest request1 = new PaymentRequest(UUID.randomUUID(), new BigDecimal("500"), "USD");
        HttpEntity<PaymentRequest> entity1 = new HttpEntity<>(request1, headers);

        ResponseEntity<PaymentResponse> response1 = restTemplate.postForEntity(
            "/api/v1/payments", entity1, PaymentResponse.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Different payload
        PaymentRequest request2 = new PaymentRequest(UUID.randomUUID(), new BigDecimal("600"), "USD");
        HttpEntity<PaymentRequest> entity2 = new HttpEntity<>(request2, headers);

        ResponseEntity<String> response2 = restTemplate.exchange(
            "/api/v1/payments", HttpMethod.POST, entity2, String.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response2.getBody()).contains("Payload tampering detected");
    }
}
