package com.vantage.order.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.query.ui.dto.OrderSearchResultResponse;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.payment.app.event.PaymentSucceededPayload;
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
import com.vantage.order.query.ui.dto.OrderSearchPageResponse;
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
@Import(OrderSearchCqrsIT.TestSecurityConfig.class)
@Testcontainers
@org.springframework.test.context.TestPropertySource(properties = {
    "vantage.inventory.consumer.enabled=true",
    "vantage.payment.enabled=true",
    "vantage.outbox.enabled=true"
})
class OrderSearchCqrsIT {

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
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_project_order_to_search_view_with_correct_product_name_and_status() throws Exception {
        TestSetup setup = setupVendorProductAndInventory();

        UUID orderId = placeOrder(setup.token(), setup.tenantId(), setup.productId(), 5);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(setup.token());
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Tenant-ID", setup.tenantId().toString());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<OrderSearchPageResponse> response = restTemplate.exchange(
                    "/api/v1/orders/search",
                    HttpMethod.GET,
                    entity,
                    OrderSearchPageResponse.class
                );

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().content()).hasSize(1);

                OrderSearchResultResponse result = response.getBody().content().get(0);
                assertThat(result.orderId()).isEqualTo(orderId);
                assertThat(result.productName()).isEqualTo("Test Product");
                assertThat(result.status()).isEqualTo("CREATED");
                assertThat(result.quantity()).isEqualTo(5);
            });
    }

    @Test
    void should_update_order_status_to_paid_when_payment_succeeds() throws Exception {
        TestSetup setup = setupVendorProductAndInventory();

        UUID orderId = placeOrder(setup.token(), setup.tenantId(), setup.productId(), 3);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(setup.token());
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Tenant-ID", setup.tenantId().toString());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<OrderSearchPageResponse> response = restTemplate.exchange(
                    "/api/v1/orders/search",
                    HttpMethod.GET,
                    entity,
                    OrderSearchPageResponse.class
                );

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().content()).hasSize(1);
                assertThat(response.getBody().content().get(0).status()).isEqualTo("CREATED");
            });

        PaymentSucceededPayload paymentPayload = new PaymentSucceededPayload(orderId, setup.tenantId());
        String jsonPayload = objectMapper.writeValueAsString(paymentPayload);
        UUID eventId = UUID.randomUUID();
        Message message = MessageBuilder
            .withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("eventId", eventId.toString())
            .build();
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, "PaymentSucceededEvent", message);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(setup.token());
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Tenant-ID", setup.tenantId().toString());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<OrderSearchPageResponse> response = restTemplate.exchange(
                    "/api/v1/orders/search",
                    HttpMethod.GET,
                    entity,
                    OrderSearchPageResponse.class
                );

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().content()).hasSize(1);
                assertThat(response.getBody().content().get(0).status()).isEqualTo("PAID");
            });
    }

    private record TestSetup(String token, UUID tenantId, UUID productId) {}

    private TestSetup setupVendorProductAndInventory() {
        UUID dummyTenantId = UUID.randomUUID();
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "cqrs_" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc."
        );
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", dummyTenantId.toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class
        );
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        ProductRequest productReq = new ProductRequest("Test Product", "Description", new BigDecimal("100.0"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, headers);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
            "/api/v1/products", productEntity, ProductResponse.class
        );
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        return new TestSetup(token, tenantId, productId);
    }

    private UUID placeOrder(String token, UUID tenantId, UUID productId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());
        OrderRequest orderReq = new OrderRequest(productId, quantity, "Test Product");
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, headers);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
            "/api/v1/orders", orderEntity, OrderResponse.class
        );
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return orderRes.getBody().id();
    }
}
