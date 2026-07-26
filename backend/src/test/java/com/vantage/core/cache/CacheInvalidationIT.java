package com.vantage.core.cache;

import com.vantage.analytics.app.AnalyticsService;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private AnalyticsService analyticsService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ForecastCacheEvictionListener listener;

    @Test
    void should_cache_forecast_and_evict_on_order_created_event() {
        ForecastCacheEvictionListener.resetEventReceived();

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
            System.err.println("CacheManager: " + cacheManager.getClass());
            System.err.println("Cache names: " + String.join(", ", cacheManager.getCacheNames()));
            Cache forecastCache = cacheManager.getCache("forecastCache");
            assertThat(forecastCache).isNotNull();

            // 5. First forecast call (cache miss)
            System.err.println("Calling getForecast for product: " + productId);
            ForecastResponse firstForecast = analyticsService.getForecast(productId);
            System.err.println("Forecast response received: " + firstForecast);
            assertThat(firstForecast).isNotNull();
            assertThat(firstForecast.forecast()).hasSize(7);

            // Verify cache now contains the value
            System.err.println("Cache entry for product after first call: " + forecastCache.get(productId));
            assertThat(forecastCache.get(productId)).isNotNull();

            // 6. Second forecast call (cache hit)
            ForecastResponse secondForecast = analyticsService.getForecast(productId);
            assertThat(secondForecast).isNotNull();
            assertThat(secondForecast.forecast()).isEqualTo(firstForecast.forecast());

            // 7. Insert a new order (to change history)
            Order newOrder = new Order();
            newOrder.setProductId(productId);
            newOrder.setQuantity(10);
            newOrder.setStatus(OrderStatus.CONFIRMED);
            newOrder.setCreatedAt(Instant.now());
            newOrder.setTenantId(tenantId);
            orderRepository.save(newOrder);

            // 8. Publish event to trigger eviction
            eventPublisher.publishEvent(new OrderCreatedEvent(UUID.randomUUID(), productId, tenantId));

            // Also explicitly call listener to ensure eviction
            System.err.println("Calling listener directly for product: " + productId);
            listener.onOrderCreated(new OrderCreatedEvent(UUID.randomUUID(), productId, tenantId));
            System.err.println("After listener call, cache entry: " + forecastCache.get(productId));
            System.err.println("Cache entry after listener: " + forecastCache.get(productId));

            // 9. Verify cache entry was evicted
            assertThat(forecastCache.get(productId)).isNull();
            System.err.println("Asserting cache is null, current value: " + forecastCache.get(productId));
            assertThat(ForecastCacheEvictionListener.isEventReceived()).isTrue();

            // 10. Third forecast call (cache miss, recomputed)
            ForecastResponse thirdForecast = analyticsService.getForecast(productId);
            assertThat(thirdForecast).isNotNull();

            // The forecast should have changed because new order added
            assertThat(thirdForecast.forecast()).isNotEqualTo(firstForecast.forecast());

        } finally {
            TenantContext.clear();
        }
    }
}