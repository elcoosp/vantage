package com.vantage.payment;
import com.vantage.AbstractIntegrationTest;

import com.vantage.core.admin.ChaosMonkeyService;
import com.vantage.payment.infrastructure.MockPaymentGatewayClient;
import com.vantage.payment.infrastructure.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BulkheadIT.TestSecurityConfig.class)
public class BulkheadIT  extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
    }

    @Autowired
    private MockPaymentGatewayClient paymentClient;

    @Autowired
    private ChaosMonkeyService chaosMonkeyService;

    @Test
    void should_limit_concurrent_payment_calls_to_5_and_fallback_immediately() throws Exception {
        // Ensure chaos monkey is disabled so we don't get artificial failures
        chaosMonkeyService.disablePaymentFailure();

        int totalCalls = 10;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Callable<PaymentResult>> tasks = new ArrayList<>();
        for (int i = 0; i < totalCalls; i++) {
            tasks.add(() -> paymentClient.processPayment(UUID.randomUUID()));
        }

        List<Future<PaymentResult>> futures = executor.invokeAll(tasks);
        List<PaymentResult> results = new ArrayList<>();
        for (Future<PaymentResult> f : futures) {
            results.add(f.get());
        }
        executor.shutdown();

        // With bulkhead maxConcurrentCalls=5, we expect exactly 5 SUCCESS and 5 GATEWAY_TIMEOUT (fallback)
        long successCount = results.stream().filter(r -> r == PaymentResult.SUCCESS).count();
        long fallbackCount = results.stream().filter(r -> r == PaymentResult.GATEWAY_TIMEOUT).count();

        // This test will fail until Bulkhead is applied
        assertThat(successCount).isEqualTo(5);
        assertThat(fallbackCount).isEqualTo(5);
    }
}
