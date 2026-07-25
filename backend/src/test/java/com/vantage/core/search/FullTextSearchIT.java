package com.vantage.core.search;

import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FullTextSearchIT.TestSecurityConfig.class)

    @BeforeEach
    void setupFTS() throws Exception {
        log.info("=== Running FTS setup ===");
        try (var conn = jdbcTemplate.getDataSource().getConnection()) {
            var resource = new ClassPathResource("db/migration/clean_fts_setup.sql");
            var script = new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try (var stmt = conn.createStatement()) {
                stmt.execute(script);
            }
            log.info("FTS setup completed successfully");
        } catch (Exception e) {
            log.error("FTS setup failed", e);
            throw e;
        }
    }


@Testcontainers
public class FullTextSearchIT {

    private static final Logger log = LoggerFactory.getLogger(FullTextSearchIT.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;



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

    private record TestContext(String token, UUID tenantId, UUID product1Id, UUID product2Id, UUID orderId) {}

    private TestContext setupVendorAndData() {
        // Register vendor A
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "fts-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vendor A");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        // Create product 1: Coffee Mug
        ProductRequest prod1 = new ProductRequest("Coffee Mug", "Ceramic coffee mug", new BigDecimal("15.99"));
        HttpEntity<ProductRequest> prod1Entity = new HttpEntity<>(prod1, headers);
        ResponseEntity<ProductResponse> prod1Res = restTemplate.postForEntity("/api/v1/products", prod1Entity, ProductResponse.class);
        assertThat(prod1Res.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID product1Id = prod1Res.getBody().id();

        // Create product 2: Coffee Beans
        ProductRequest prod2 = new ProductRequest("Coffee Beans", "Premium roasted beans", new BigDecimal("25.99"));
        HttpEntity<ProductRequest> prod2Entity = new HttpEntity<>(prod2, headers);
        ResponseEntity<ProductResponse> prod2Res = restTemplate.postForEntity("/api/v1/products", prod2Entity, ProductResponse.class);
        assertThat(prod2Res.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID product2Id = prod2Res.getBody().id();

        // Set inventory for product1 (needed for order)
        InventoryUpdateRequest invReq = new InventoryUpdateRequest(10);
        HttpHeaders invHeaders = new HttpHeaders();
        invHeaders.setBearerAuth(token);
        invHeaders.setContentType(MediaType.APPLICATION_JSON);
        invHeaders.set("X-Tenant-ID", tenantId.toString());
        invHeaders.setIfMatch("0");
        HttpEntity<InventoryUpdateRequest> invEntity = new HttpEntity<>(invReq, invHeaders);
        ResponseEntity<InventoryResponse> invRes = restTemplate.exchange(
            "/api/v1/inventory/" + product1Id, HttpMethod.PUT, invEntity, InventoryResponse.class);
        assertThat(invRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Create order (with status CREATED)
        com.vantage.order.ui.dto.OrderRequest orderReq = new com.vantage.order.ui.dto.OrderRequest(product1Id, 2);
        HttpEntity<com.vantage.order.ui.dto.OrderRequest> orderEntity = new HttpEntity<>(orderReq, headers);
        ResponseEntity<com.vantage.order.ui.dto.OrderResponse> orderRes = restTemplate.postForEntity("/api/v1/orders", orderEntity, com.vantage.order.ui.dto.OrderResponse.class);
        assertThat(orderRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID orderId = orderRes.getBody().id();

        return new TestContext(token, tenantId, product1Id, product2Id, orderId);
    }

    @Test
    void should_return_products_and_orders_matching_search_term() {
        TestContext ctx = setupVendorAndData();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ctx.token);
        headers.set("X-Tenant-ID", ctx.tenantId.toString());

        // Search for "Coffee" should return both products
        ResponseEntity<List> response = restTemplate.exchange(
            "/api/v1/search?q=Coffee",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> results = response.getBody();
        assertThat(results).hasSize(2);
        // Verify entity types and ids
        // (We expect both products)
        List<String> entityTypes = results.stream().map(m -> (String) m.get("entityType")).toList();
        assertThat(entityTypes).containsOnly("PRODUCT");
        List<UUID> ids = results.stream().map(m -> UUID.fromString((String) m.get("id"))).toList();
        assertThat(ids).containsExactlyInAnyOrder(ctx.product1Id, ctx.product2Id);

        // Search for "Mug" should return only Coffee Mug
        response = restTemplate.exchange(
            "/api/v1/search?q=Mug",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        results = response.getBody();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("id")).isEqualTo(ctx.product1Id.toString());
        assertThat(results.get(0).get("entityType")).isEqualTo("PRODUCT");

        // Search for "CREATED" should return the order (status)
        response = restTemplate.exchange(
            "/api/v1/search?q=CREATED",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        results = response.getBody();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("id")).isEqualTo(ctx.orderId.toString());
        assertThat(results.get(0).get("entityType")).isEqualTo("ORDER");
    }

    @Test
    void should_respect_tenant_isolation() {
        // Setup vendor A (same as above) and vendor B
        TestContext ctxA = setupVendorAndData();

        // Register vendor B
        VendorRegistrationRequest vendorReqB = new VendorRegistrationRequest(
            "fts-b-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vendor B");
        HttpHeaders vendorHeadersB = new HttpHeaders();
        vendorHeadersB.setContentType(MediaType.APPLICATION_JSON);
        vendorHeadersB.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntityB = new HttpEntity<>(vendorReqB, vendorHeadersB);
        ResponseEntity<AuthResponse> vendorResB = restTemplate.postForEntity("/api/v1/vendors/register", vendorEntityB, AuthResponse.class);
        assertThat(vendorResB.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tokenB = vendorResB.getBody().token();
        UUID tenantIdB = vendorResB.getBody().tenantId();

        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        headersB.setContentType(MediaType.APPLICATION_JSON);
        headersB.set("X-Tenant-ID", tenantIdB.toString());

        // Create product for vendor B: "Tea Mug"
        ProductRequest prodB = new ProductRequest("Tea Mug", "Porcelain tea mug", new BigDecimal("12.99"));
        HttpEntity<ProductRequest> prodBEntity = new HttpEntity<>(prodB, headersB);
        ResponseEntity<ProductResponse> prodBRes = restTemplate.postForEntity("/api/v1/products", prodBEntity, ProductResponse.class);
        assertThat(prodBRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID productBId = prodBRes.getBody().id();

        // Search as vendor A for "Mug" should return only A's Coffee Mug, not B's Tea Mug
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(ctxA.token);
        headersA.set("X-Tenant-ID", ctxA.tenantId.toString());

        ResponseEntity<List> responseA = restTemplate.exchange(
            "/api/v1/search?q=Mug",
            HttpMethod.GET,
            new HttpEntity<>(headersA),
            List.class
        );
        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> resultsA = responseA.getBody();
        assertThat(resultsA).hasSize(1);
        assertThat(resultsA.get(0).get("id")).isEqualTo(ctxA.product1Id.toString()); // Coffee Mug

        // Search as vendor B for "Mug" should return only B's Tea Mug
        ResponseEntity<List> responseB = restTemplate.exchange(
            "/api/v1/search?q=Mug",
            HttpMethod.GET,
            new HttpEntity<>(headersB),
            List.class
        );
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> resultsB = responseB.getBody();
        assertThat(resultsB).hasSize(1);
        assertThat(resultsB.get(0).get("id")).isEqualTo(productBId.toString());
    }
}
