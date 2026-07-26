package com.vantage.core;

import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.product.domain.Product;
import com.vantage.product.domain.ProductRepository;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.show-sql=true"
})
@Testcontainers
public class QueryOptimizationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_fetch_orders_with_products_in_single_query() {
        UUID tenantId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setEmail("test-opt@vantage.com");
        vendor.setPasswordHash("hash");
        vendor.setCompanyName("Test Co");
        vendorRepository.save(vendor);

        TenantContext.setTenantId(tenantId);
        try {
            Product product = new Product();
            product.setName("Test Product");
            product.setDescription("Description");
            product.setPrice(BigDecimal.TEN);
            product.setSku("SKU-001");
            productRepository.save(product);

            for (int i = 0; i < 100; i++) {
                Order order = new Order();
                order.setProductId(product.getId());
                order.setQuantity(1);
                order.setStatus(OrderStatus.CREATED);
                orderRepository.save(order);
            }
        } finally {
            TenantContext.clear();
        }

        entityManager.clear();
        Statistics stats = entityManager.getSessionFactory().getStatistics();
        stats.clear();

        TenantContext.setTenantId(tenantId);
        try {
            var orders = orderRepository.findAllWithProduct();
            assertThat(orders).hasSize(100);

            boolean found = false;
            for (String query : stats.getQueries()) {
                if (query.contains("JOIN FETCH")) {
                    QueryStatistics queryStats = stats.getQueryStatistics(query);
                    assertThat(queryStats.getExecutionCount()).isEqualTo(1);
                    found = true;
                }
            }
            assertThat(found).isTrue();
        } finally {
            TenantContext.clear();
        }
    }
}
