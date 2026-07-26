// backend/src/test/java/com/vantage/product/BulkUploadIT.java
package com.vantage.product;

import com.vantage.product.ui.dto.BulkUploadResponse;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BulkUploadIT.TestSecurityConfig.class)
@Testcontainers
public class BulkUploadIT {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_process_bulk_upload_with_virtual_threads_and_return_summary() {
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "bulk-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc."
        );
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class
        );
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("name,price,sku\n");
        for (int i = 1; i <= 1000; i++) {
            csvContent.append("Product ").append(i).append(",").append(i).append(".99,SKU").append(i).append("\n");
        }
        csvContent.append(",10.00,SKU1001\n");
        csvContent.append("Product 1002,-5.00,SKU1002\n");
        csvContent.append("Product 1003,0.00,SKU1003\n");
        csvContent.append(",,\n");
        csvContent.append("Product 1005,abc,SKU1005\n");

        ByteArrayResource resource = new ByteArrayResource(csvContent.toString().getBytes()) {
            @Override
            public String getFilename() {
                return "products.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        headers.set("X-Tenant-ID", tenantId.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        long startTime = System.currentTimeMillis();
        ResponseEntity<BulkUploadResponse> response = restTemplate.postForEntity(
            "/api/v1/products/bulk-upload", requestEntity, BulkUploadResponse.class
        );
        long duration = System.currentTimeMillis() - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        BulkUploadResponse bulkResponse = response.getBody();
        assertThat(bulkResponse.totalRecords()).isEqualTo(1005);
        assertThat(bulkResponse.successCount()).isEqualTo(1000);
        assertThat(bulkResponse.failureCount()).isEqualTo(5);
        assertThat(bulkResponse.errors()).hasSize(5);
        assertThat(duration).isLessThan(5000);

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM products WHERE tenant_id = ?", Integer.class, tenantId
        );
        assertThat(count).isEqualTo(1000);
    }
}
