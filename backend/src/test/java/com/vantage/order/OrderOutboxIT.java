// backend/src/test/java/com/vantage/order/OrderOutboxIT.java
package com.vantage.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.app.event.OrderCreatedPayload;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OrderOutboxIT.TestSecurityConfig.class)
@Testcontainers
public class OrderOutboxIT {

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
    private OutboxRepository outboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_persist_order_and_outbox_and_publish_to_rabbitmq_when_creating_order() throws Exception {
        UUID dummyTenantId = UUID.randomUUID();
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest("order@vantage.com", "securePassword123", "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", dummyTenantId.toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).as("Vendor registration failed: %s", vendorRes).isEqualTo(HttpStatus.CREATED);
        assertThat(vendorRes.getBody()).isNotNull();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Order Product", "Description", new BigDecimal("50.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).as("Product creation failed: %s", productRes).isEqualTo(HttpStatus.OK);
        assertThat(productRes.getBody()).isNotNull();
        UUID productId = productRes.getBody().id();

        OrderRequest orderReq = new OrderRequest(productId, 5);
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, headers);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity("/api/v1/orders", orderEntity, OrderResponse.class);

        assertThat(orderRes.getStatusCode()).as("Order creation failed: %s", orderRes).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(orderRes.getBody()).isNotNull();
        UUID orderId = orderRes.getBody().id();

        TenantContext.setTenantId(tenantId);
        try {
            assertThat(orderRepository.findById(orderId)).isPresent();

            OutboxEvent pendingEvent = outboxRepository.findByAggregateId(orderId).orElseThrow();
            assertThat(pendingEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(pendingEvent.getEventType()).isEqualTo("OrderCreatedEvent");
            assertThat(pendingEvent.getAggregateType()).isEqualTo("ORDER");
            assertThat(pendingEvent.getPayload()).contains(orderId.toString());

            Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    OutboxEvent event = outboxRepository.findByAggregateId(orderId).orElseThrow();
                    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
                    assertThat(event.getPublishedAt()).isNotNull();
                });

            Message message = rabbitTemplate.receive(RabbitMQConfig.QUEUE, 5000);
            assertThat(message).as("Message should be present in RabbitMQ").isNotNull();
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            OrderCreatedPayload payload = objectMapper.readValue(body, OrderCreatedPayload.class);
            assertThat(payload.orderId()).isEqualTo(orderId);
            assertThat(payload.tenantId()).isEqualTo(tenantId);
            assertThat(payload.productId()).isEqualTo(productId);
            assertThat(payload.quantity()).isEqualTo(5);
        } finally {
            TenantContext.clear();
        }
    }
}
