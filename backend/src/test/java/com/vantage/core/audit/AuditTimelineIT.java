package com.vantage.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.audit.ui.dto.AuditEventResponse;

import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
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
// import removed - using fully qualified @org.springframework.core.annotation.Order
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
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AuditTimelineIT.TestSecurityConfig.class)
@Testcontainers
public class AuditTimelineIT {


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
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Test
    void should_capture_order_created_and_order_updated_events_and_return_timeline() throws Exception {
        // 1. Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "audit-" + UUID.randomUUID() + "@vantage.com",
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

        ProductRequest productReq = new ProductRequest("Audit Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
                "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // 3. Place order
        OrderRequest orderReq = new OrderRequest(productId, 2);
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, authHeaders);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
                "/api/v1/orders", orderEntity, OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID orderId = orderRes.getBody().id();

        // 4. Simulate payment failure: update order status to CANCELLED
        TenantContext.setTenantId(tenantId);
        try {
            com.vantage.order.domain.Order order = orderRepository.findById(orderId).orElseThrow();
            System.out.println("Updating order " + orderId + " status to CANCELLED");
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            System.out.println("Order updated");
            // Debug: check entity_events table directly
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM entity_events WHERE aggregate_id = ?",
                Integer.class, orderId);
            System.out.println("Direct DB count of entity_events for order: " + count);
        } finally {
            TenantContext.clear();
        }

        // 5. Call audit endpoint
        HttpHeaders auditHeaders = new HttpHeaders();
        auditHeaders.setBearerAuth(token);
        auditHeaders.set("X-Tenant-ID", tenantId.toString());

        ResponseEntity<AuditEventResponse[]> auditRes = restTemplate.exchange(
                "/api/v1/audit/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(auditHeaders),
                AuditEventResponse[].class);
        assertThat(auditRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AuditEventResponse> events = List.of(auditRes.getBody());
        System.out.println("Audit events count: " + events.size());
        for (AuditEventResponse e : events) {
            System.out.println("Event: " + e.eventType() + " at " + e.createdAt() + " payload: " + e.payload());
        }
        // Also query DB directly
        Integer dbCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM entity_events WHERE aggregate_id = ?",
            Integer.class, orderId);
        System.out.println("Direct DB count after GET: " + dbCount);

        // 6. Verify exactly two events, sorted by createdAt ascending
        assertThat(events).hasSize(2);
        AuditEventResponse first = events.get(0);
        AuditEventResponse second = events.get(1);

        assertThat(first.eventType()).isEqualTo("ORDER_CREATED");
        assertThat(second.eventType()).isEqualTo("ORDER_UPDATED");

        // Verify payload of second event reflects CANCELLED status
        String payload = second.payload();
        assertThat(payload).contains("\"status\":\"CANCELLED\"");
    }
}
