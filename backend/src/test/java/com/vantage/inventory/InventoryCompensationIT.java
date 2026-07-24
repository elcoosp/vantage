package com.vantage.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import com.vantage.order.app.event.InventoryReleasedPayload;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(InventoryCompensationIT.TestSecurityConfig.class)
@Testcontainers
@org.springframework.test.context.TestPropertySource(properties = "vantage.inventory.compensation.enabled=true")
class InventoryCompensationIT {

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
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_restore_inventory_quantity_and_remain_idempotent_when_inventory_released_event_is_republished() throws Exception {
        UUID eventId = UUID.randomUUID();
        TestSetup setup = setupProductAndInventory(10);

        publishInventoryReleasedEvent(eventId, setup.tenantId(), setup.productId(), 4);

        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                TenantContext.setTenantId(setup.tenantId());
                try {
                    Inventory inventory = inventoryRepository.findByProductId(setup.productId()).orElseThrow();
                    assertThat(inventory.getQuantity()).isEqualTo(14);
                    assertThat(processedEventRepository.existsById(eventId)).isTrue();
                } finally {
                    TenantContext.clear();
                }
            });

        publishInventoryReleasedEvent(eventId, setup.tenantId(), setup.productId(), 4);

        Awaitility.await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                TenantContext.setTenantId(setup.tenantId());
                try {
                    Inventory inventory = inventoryRepository.findByProductId(setup.productId()).orElseThrow();
                    assertThat(inventory.getQuantity()).isEqualTo(14);
                    assertThat(processedEventRepository.existsById(eventId)).isTrue();
                } finally {
                    TenantContext.clear();
                }
            });
    }

    private record TestSetup(UUID tenantId, UUID productId) {}

    private TestSetup setupProductAndInventory(int initialQuantity) {
        UUID dummyTenantId = UUID.randomUUID();
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "compensation_" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", dummyTenantId.toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
            "/api/v1/products", productEntity, ProductResponse.class);
        UUID productId = productRes.getBody().id();

        HttpHeaders updateHeaders = new HttpHeaders();
        updateHeaders.setBearerAuth(token);
        updateHeaders.setContentType(MediaType.APPLICATION_JSON);
        updateHeaders.set("X-Tenant-ID", tenantId.toString());
        updateHeaders.setIfMatch("0");
        InventoryUpdateRequest initReq = new InventoryUpdateRequest(initialQuantity);
        HttpEntity<InventoryUpdateRequest> initEntity = new HttpEntity<>(initReq, updateHeaders);
        ResponseEntity<InventoryResponse> initRes = restTemplate.exchange(
            "/api/v1/inventory/" + productId, HttpMethod.PUT, initEntity, InventoryResponse.class);
        assertThat(initRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initRes.getBody()).isNotNull();
        assertThat(initRes.getBody().quantity()).isEqualTo(initialQuantity);

        return new TestSetup(tenantId, productId);
    }

    private void publishInventoryReleasedEvent(UUID eventId, UUID tenantId, UUID productId, int releasedQuantity) throws Exception {
        InventoryReleasedPayload payload = new InventoryReleasedPayload(
            UUID.randomUUID(), tenantId, productId, releasedQuantity);
        String jsonPayload = objectMapper.writeValueAsString(payload);
        Message message = MessageBuilder
            .withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("eventId", eventId.toString())
            .build();
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, "InventoryReleasedEvent", message);
    }
}
