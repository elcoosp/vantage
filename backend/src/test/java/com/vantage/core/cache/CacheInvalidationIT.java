package com.vantage.core.cache;

import com.vantage.analytics.messaging.ForecastCacheEvictionListener;
import com.vantage.analytics.messaging.OrderCreatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CacheInvalidationIT.TestSecurityConfig.class)
@Testcontainers
public class CacheInvalidationIT {

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
    private OrderRepository orderRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void should_cache_forecast_and_evict_on_order_created_event() {
        // Reset flag
        ForecastCacheEvictionListener.resetEventReceived();

        // 1. Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "cache-" + UUID.randomUUID() + "@vantage.com",
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

        ProductRequest productReq = new ProductRequest("Cache Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // 3. Insert some historical orders (30 days) to get meaningful forecast
        TenantContext.setTenantId(tenantId);
        try {
            LocalDate start = LocalDate.now().minusDays(30);
            for (int i = 0; i < 30; i++) {
                LocalDate date = start.plusDays(i);
                int quantity = 2 + (i % 3);
                Order order = new Order();
                order.setProductId(productId);
                order.setQuantity(quantity);
                order.setStatus(OrderStatus.CONFIRMED);
                Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);
                order.setCreatedAt(instant);
                order.setTenantId(tenantId);
                orderRepository.save(order);
            }
        } finally {
            TenantContext.clear();
        }

        // 4. First forecast call (cache miss)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Tenant-ID", tenantId.toString());

        ResponseEntity<ForecastResponse> firstResponse = restTemplate.exchange(
                "/api/v1/analytics/forecast/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ForecastResponse.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isNotNull();
        ForecastResponse firstForecast = firstResponse.getBody();

        // 5. Second forecast call (should be cache hit - same result)
        ResponseEntity<ForecastResponse> secondResponse = restTemplate.exchange(
                "/api/v1/analytics/forecast/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ForecastResponse.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getBody()).isNotNull();
        ForecastResponse secondForecast = secondResponse.getBody();
        // Same result (cached)
        assertThat(secondForecast.forecast()).isEqualTo(firstForecast.forecast());

        // 6. Insert a new order (to change history)
        TenantContext.setTenantId(tenantId);
        try {
            Order newOrder = new Order();
            newOrder.setProductId(productId);
            newOrder.setQuantity(10);
            newOrder.setStatus(OrderStatus.CONFIRMED);
            newOrder.setCreatedAt(Instant.now());
            newOrder.setTenantId(tenantId);
            orderRepository.save(newOrder);
        } finally {
            TenantContext.clear();
        }

        // Publish event to trigger eviction
        eventPublisher.publishEvent(new OrderCreatedEvent(this, UUID.randomUUID(), productId, tenantId));

        // Wait a moment for event processing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that the event was received
        System.out.println("Flag after publish: " + ForecastCacheEvictionListener.isEventReceived());
        assertThat(ForecastCacheEvictionListener.isEventReceived()).isTrue();

        // 7. Third forecast call (should be cache miss, recomputed)
        ResponseEntity<ForecastResponse> thirdResponse = restTemplate.exchange(
                "/api/v1/analytics/forecast/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ForecastResponse.class);
        assertThat(thirdResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(thirdResponse.getBody()).isNotNull();
        ForecastResponse thirdForecast = thirdResponse.getBody();

        // The forecast should have changed because new order added
        assertThat(thirdForecast.forecast()).isNotEqualTo(firstForecast.forecast());
    }
}