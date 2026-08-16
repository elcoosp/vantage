package com.vantage.analytics;

import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ForecastAnalyticsIT.TestSecurityConfig.class)
@Testcontainers
public class ForecastAnalyticsIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @org.springframework.core.annotation.Order(1)
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
    private OrderRepository orderRepository;

    @Test
    void should_return_forecast_for_product_with_7_data_points_and_valid_bounds() {
        // 1. Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "forecast-" + UUID.randomUUID() + "@vantage.com",
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

        // 2. Create product
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Forecast Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // 3. Insert 30 days of orders with varying quantities (weekend higher)
        TenantContext.setTenantId(tenantId);
        try {
            LocalDate start = LocalDate.now().minusDays(30);
            for (int i = 0; i < 30; i++) {
                LocalDate date = start.plusDays(i);
                int quantity;
                // Saturday (6) or Sunday (0) -> higher
                int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday ... 7=Sunday
                if (dayOfWeek == 6 || dayOfWeek == 7) {
                    quantity = 10 + (i % 3); // some variation
                } else {
                    quantity = 2 + (i % 2);
                }
                Order order = new Order();
                order.setProductId(productId);
                order.setQuantity(quantity);
                order.setStatus(OrderStatus.CONFIRMED);
                // Set created_at to the specific date
                Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);
                order.setCreatedAt(instant);
                order.setTenantId(tenantId);
                orderRepository.save(order);
            }
        } finally {
            TenantContext.clear();
        }

        // 4. Call forecast endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Tenant-ID", tenantId.toString());

        ResponseEntity<ForecastResponse> response = restTemplate.exchange(
                "/api/v1/analytics/forecast/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ForecastResponse.class);

        // 5. Assertions
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        ForecastResponse forecast = response.getBody();
        assertThat(forecast.forecast()).hasSize(7);
        for (ForecastDataPoint point : forecast.forecast()) {
            assertThat(point.predictedQuantity()).isNotNull();
            assertThat(point.lowerBound()).isNotNull();
            assertThat(point.upperBound()).isNotNull();
            assertThat(point.lowerBound()).isGreaterThanOrEqualTo(0);
            assertThat(point.upperBound()).isGreaterThanOrEqualTo(point.predictedQuantity());
            assertThat(point.lowerBound()).isLessThanOrEqualTo(point.predictedQuantity());
            assertThat(point.date()).isNotNull();
        }
    }
}
