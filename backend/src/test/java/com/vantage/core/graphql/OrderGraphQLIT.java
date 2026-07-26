package com.vantage.core.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.awaitility.Awaitility;
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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OrderGraphQLIT.TestSecurityConfig.class)
@Testcontainers
@TestPropertySource(properties = "vantage.outbox.enabled=true")
public class OrderGraphQLIT {

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
    private ObjectMapper objectMapper;

    @Test
    void should_return_orders_with_requested_fields_when_querying_graphql() throws Exception {
        // 1. Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "graphql-" + UUID.randomUUID() + "@vantage.com",
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
        ProductRequest productReq = new ProductRequest("GraphQL Product", "Description", new BigDecimal("99.99"));
        HttpEntity<ProductRequest> productEntity = new HttpEntity<>(productReq, authHeaders);
        ResponseEntity<ProductResponse> productRes = restTemplate.postForEntity(
            "/api/v1/products", productEntity, ProductResponse.class);
        assertThat(productRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productId = productRes.getBody().id();

        // 3. Place order
        OrderRequest orderReq = new OrderRequest(productId, 5, "GraphQL Product");
        HttpEntity<OrderRequest> orderEntity = new HttpEntity<>(orderReq, authHeaders);
        ResponseEntity<OrderResponse> orderRes = restTemplate.postForEntity(
            "/api/v1/orders", orderEntity, OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID orderId = orderRes.getBody().id();

        // 4. Wait for CQRS projection
        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .untilAsserted(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Tenant-ID", tenantId.toString());
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<String> searchRes = restTemplate.exchange(
                    "/api/v1/orders/search",
                    HttpMethod.GET,
                    entity,
                    String.class);
                assertThat(searchRes.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(searchRes.getBody()).contains(orderId.toString());
            });

        // 5. Send GraphQL query
        String graphqlQuery = """
            {
                "query": "query { orders(status: \\"CREATED\\") { orderId status } }"
            }
            """;

        HttpHeaders graphqlHeaders = new HttpHeaders();
        graphqlHeaders.setBearerAuth(token);
        graphqlHeaders.setContentType(MediaType.APPLICATION_JSON);
        graphqlHeaders.set("X-Tenant-ID", tenantId.toString());
        HttpEntity<String> graphqlEntity = new HttpEntity<>(graphqlQuery, graphqlHeaders);

        ResponseEntity<String> graphqlRes = restTemplate.exchange(
            "/graphql",
            HttpMethod.POST,
            graphqlEntity,
            String.class);

        assertThat(graphqlRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseJson = objectMapper.readTree(graphqlRes.getBody());
        assertThat(responseJson.has("data")).isTrue();
        assertThat(responseJson.get("data").has("orders")).isTrue();

        JsonNode orders = responseJson.get("data").get("orders");
        assertThat(orders.isArray()).isTrue();
        assertThat(orders.size()).isGreaterThanOrEqualTo(1);

        boolean foundOrder = false;
        for (JsonNode order : orders) {
            if (order.get("orderId").asText().equals(orderId.toString())) {
                foundOrder = true;
                assertThat(order.get("status").asText()).isEqualTo("CREATED");
                assertThat(order.size()).isEqualTo(2);
                break;
            }
        }
        assertThat(foundOrder).isTrue();
    }
}
