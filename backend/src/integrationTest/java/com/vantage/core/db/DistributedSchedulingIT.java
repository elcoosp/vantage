// backend/src/test/java/com/vantage/core/db/DistributedSchedulingIT.java
package com.vantage.core.db;

import org.springframework.transaction.annotation.Transactional;

import com.vantage.core.messaging.app.OutboxPoller;
import com.vantage.core.messaging.domain.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class DistributedSchedulingIT {

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
        registry.add("vantage.outbox.enabled", () -> "true");
    }

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private OutboxPoller outboxPoller;

    @MockitoSpyBean
    private OutboxRepository outboxRepository;

    @Test
    void should_acquire_and_release_lock() {
        String lockName = "test_lock_" + java.util.UUID.randomUUID().toString();

        boolean first = distributedLockService.tryAcquireLock(lockName);
        System.out.println("First acquire returned: " + first);
        assertThat(first).isTrue();

        // Within the same session, the lock is already held, so tryAcquireLock should return true
        boolean second = distributedLockService.tryAcquireLock(lockName);
        System.out.println("Second acquire (same session) returned: " + second);
        assertThat(second).isTrue();

        distributedLockService.releaseLock(lockName);

        // After release, we can acquire again
        boolean third = distributedLockService.tryAcquireLock(lockName);
        System.out.println("Third acquire (after release) returned: " + third);
        assertThat(third).isTrue();

        distributedLockService.releaseLock(lockName);
    }

    @Test
    void should_only_one_poller_execute_when_concurrent() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    System.out.println("Thread starting pollAndPublish");
                    outboxPoller.pollAndPublish();
                    System.out.println("Thread finished pollAndPublish");
                } catch (Exception e) {
                    System.err.println("Thread exception: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        // Only one thread should have successfully acquired the lock and thus called the repository
        verify(outboxRepository, times(1)).findByStatus(com.vantage.core.messaging.domain.OutboxStatus.PENDING);
    }


}