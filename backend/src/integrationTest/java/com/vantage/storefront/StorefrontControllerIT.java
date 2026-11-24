package com.vantage.storefront;

import com.vantage.VantageApplication;
import com.vantage.AbstractIntegrationTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = VantageApplication.class)
@Import({StorefrontControllerIT.TestSecurityConfig.class, AbstractIntegrationTest.TestRestClientConfig.class})
public class StorefrontControllerIT  extends AbstractIntegrationTest {

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
    private RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    private record VendorCredentials(String token, UUID tenantId) {}

    private VendorCredentials registerVendor() {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "storefront-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
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
        AuthResponse body = vendorRes.getBody();
        return new VendorCredentials(body.token(), body.tenantId());
    }

    @Test
    void should_return_empty_layout_when_no_config_exists() throws Exception {
        VendorCredentials creds = registerVendor();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(creds.token);
        headers.set("X-Tenant-ID", creds.tenantId.toString());

        ResponseEntity<String> response = restClient.get()
                .uri("/api/v1/storefront")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.has("components")).isTrue();
        JsonNode components = json.get("components");
        assertThat(components.isArray()).isTrue();
        assertThat(components.size()).isEqualTo(0);
    }

    @Test
    void should_update_layout_and_return_updated() throws Exception {
        VendorCredentials creds = registerVendor();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(creds.token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", creds.tenantId.toString());

        // Updated layout as JSON array
        List<Map<String, Object>> layout = List.of(
                Map.of("componentType", "HeroBanner", "props", Map.of("title", "Welcome", "imageUrl", "http://example.com/img.jpg")),
                Map.of("componentType", "ProductGrid", "props", Map.of("title", "Featured", "limit", 4))
        );
        String payload = objectMapper.writeValueAsString(layout);

        ResponseEntity<String> putResponse = restClient.method(HttpMethod.PUT)
                .uri("/api/v1/storefront")
                .headers(h -> h.addAll(headers))
                .body(payload)
                .retrieve()
                .toEntity(String.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String putBody = putResponse.getBody();
        System.err.println("PUT response body: " + putBody);

        // GET to verify persistence
        ResponseEntity<String> getResponse = restClient.get()
                .uri("/api/v1/storefront")
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String getBody = getResponse.getBody();
        System.err.println("GET response body: " + getBody);

        // Compare as JSON objects to ignore property order
        JsonNode putJson = objectMapper.readTree(putBody);
        JsonNode getJson = objectMapper.readTree(getBody);
        assertThat(putJson).isEqualTo(getJson);
    }
}