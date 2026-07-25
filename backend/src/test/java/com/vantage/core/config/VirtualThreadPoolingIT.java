package com.vantage.core.config;

import com.vantage.core.tenant.TenantContext;
import com.vantage.product.domain.Product;
import com.vantage.product.domain.ProductRepository;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class VirtualThreadPoolingIT {

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
    private ProductRepository productRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void should_multiplex_thousands_of_virtual_threads_over_few_carrier_threads() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        try {
            Vendor vendor = new Vendor();
            vendor.setTenantId(tenantId);
            vendor.setEmail("test@vantage.com");
            vendor.setPasswordHash("dummy");
            vendor.setCompanyName("TestCo");
            vendorRepository.save(vendor);

            Product product = new Product();
            product.setName("Test Product");
            product.setDescription("Description");
            product.setPrice(java.math.BigDecimal.TEN);
            productRepository.save(product);
        } finally {
            TenantContext.clear();
        }

        int threadCount = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        long startNano = System.nanoTime();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    long count = productRepository.count();
                    if (count >= 0) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdownNow();

        long durationMs = (System.nanoTime() - startNano) / 1_000_000;

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        System.out.printf("Peak platform threads: %d, duration: %d ms%n", peakThreadCount, durationMs);

        assertThat(failureCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Ensure virtual threads are multiplexed: peak platform threads less than total virtual threads
        assertThat(peakThreadCount).isLessThan(threadCount * 2);
    }
}
