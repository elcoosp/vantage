package com.vantage.core.cache;

import com.vantage.analytics.messaging.ForecastCacheEvictionListener;
import com.vantage.analytics.messaging.OrderCreatedEvent;
import com.vantage.analytics.ui.dto.ForecastResponse;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.product.app.ProductService;
import com.vantage.product.ui.dto.ProductRequest;
import com.vantage.product.ui.dto.ProductResponse;
import com.vantage.vendor.app.VendorService;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
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
@SpringBootTest
@Import(CacheConfig.class)
@Testcontainers
public class CacheInvalidationIT {

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
    private VendorService vendorService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ForecastCacheEvictionListener forecastCacheEvictionListener;

    @Test
    void should_evict_forecast_cache_when_order_created_event_received() {
        // 1. Register vendor
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
                "cache-" + UUID.randomUUID() + "@vantage.com",
                "securePassword123",
                "Vantage Inc.");
        var registration = vendorService.register(vendorReq);
        UUID tenantId = registration.tenantId();

        TenantContext.setTenantId(tenantId);
        try {
            // 2. Create product
            ProductRequest productReq = new ProductRequest("Cache Product", "Description", new BigDecimal("99.99"));
            ProductResponse productRes = productService.createProduct(productReq);
            UUID productId = productRes.id();

            // 3. Insert some historical orders (30 days)
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

            // 4. Get the forecast cache
            Cache forecastCache = cacheManager.getCache("forecastCache");
            assertThat(forecastCache).isNotNull();

            // 5. Manually populate the cache with a dummy forecast value
            // (This simulates the cache being populated by a previous call)
            ForecastResponse dummyForecast = new ForecastResponse(java.util.List.of());
            forecastCache.put(productId, dummyForecast);
            assertThat(forecastCache.get(productId)).isNotNull();

            // 6. Insert a new order (to trigger eviction)
            Order newOrder = new Order();
            newOrder.setProductId(productId);
            newOrder.setQuantity(10);
            newOrder.setStatus(OrderStatus.CONFIRMED);
            newOrder.setCreatedAt(Instant.now());
            newOrder.setTenantId(tenantId);
            orderRepository.save(newOrder);

            // 7. Invoke the listener directly (simulating event)
            forecastCacheEvictionListener.onOrderCreated(
                new OrderCreatedEvent(UUID.randomUUID(), productId, tenantId)
            );

            // 8. Verify cache entry was evicted
            assertThat(forecastCache.get(productId)).isNull();

        } finally {
            TenantContext.clear();
        }
    }
}
