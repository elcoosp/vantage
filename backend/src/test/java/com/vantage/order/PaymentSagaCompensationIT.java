package com.vantage.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.order.app.event.InventoryReleasedPayload;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.payment.infrastructure.MockPaymentGatewayClient;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PaymentSagaCompensationIT.TestSecurityConfig.class)
@Testcontainers
@org.springframework.test.context.TestPropertySource(properties = {
    "vantage.inventory.consumer.enabled=true",
    "vantage.payment.enabled=true"
})
class PaymentSagaCompensationIT {

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockPaymentGatewayClient mockPaymentGatewayClient;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupQueues() {
        Queue releaseQueue = QueueBuilder.durable("vantage.inventory.release").build();
        rabbitAdmin.declareQueue(releaseQueue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(releaseQueue).to(new org.springframework.amqp.core.DirectExchange("vantage.events")).with("InventoryReleasedEvent"));
    }

    @Test
    void should_cancel_order_and_release_inventory_when_payment_fails() throws Exception {
        TestSetup setup = setupProductAndInventory(10);
        mockPaymentGatewayClient.setSimulateFailure(true);

        UUID orderId = placeOrder(setup.token(), setup.tenantId(), setup.productId(), 2);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                TenantContext.setTenantId(setup.tenantId());
                try {
                    assertThat(orderRepository.findById(orderId)).isPresent()
                        .get().extracting("status").isEqualTo(OrderStatus.CANCELLED);
                    Integer paymentFailedCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'PaymentFailedEvent' AND status = 'PUBLISHED'",
                        Integer.class, orderId);
                    assertThat(paymentFailedCount).isEqualTo(1);
                } finally {
                    TenantContext.clear();
                }
            });

        Message inventoryReleasedMsg = rabbitTemplate.receive("vantage.inventory.release", 5000);
        assertThat(inventoryReleasedMsg).isNotNull();
        String inventoryBody = new String(inventoryReleasedMsg.getBody(), StandardCharsets.UTF_8);
        InventoryReleasedPayload inventoryPayload = objectMapper.readValue(inventoryBody, InventoryReleasedPayload.class);
        assertThat(inventoryPayload.orderId()).isEqualTo(orderId);
        assertThat(inventoryPayload.releasedQuantity()).isEqualTo(2);

        TenantContext.setTenantId(setup.tenantId());
        try {
            Inventory inventory = inventoryRepository.findByProductId(setup.productId()).orElseThrow();
            inventory.setQuantity(inventory.getQuantity() + inventoryPayload.releasedQuantity());
            inventoryRepository.save(inventory);
            assertThat(inventory.getQuantity()).isEqualTo(10);
        } finally {
            TenantContext.clear();
        }
    }

    private record TestSetup(String token, UUID tenantId, UUID productId) {}

    private TestSetup setupProductAndInventory(int initialQuantity) {
        UUID dummyTenantId = UUID.randomUUID();
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest("test_" + UUID.randomUUID() + "@vantage.com", "securePassword123", "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", dummyTenantId.toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity("/api/v1/products", productEntity, ProductResponse.class);
        UUID productId = productRes.getBody().id();

        HttpHeaders updateHeaders = new HttpHeaders();
        updateHeaders.setBearerAuth(token);
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        updateHeaders.setIfMatch("0");
        InventoryUpdateRequest initReq = new InventoryUpdateRequest(initialQuantity);
        HttpEntity<InventoryUpdateRequest> initEntity = new HttpEntity<>(initReq, updateHeaders);
        restTemplate.exchange("/api/v1/inventory/" + productId, HttpMethod.PUT, initEntity, InventoryResponse.class);

        return new TestSetup(token, tenantId, productId);
    }

    private UUID placeOrder(String token, UUID tenantId, UUID productId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        OrderRequest orderReq = new OrderRequest(productId, quantity);
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, headers);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity("/api/v1/orders", orderEntity, OrderResponse.class);
        return orderRes.getBody().id();
    }
}
