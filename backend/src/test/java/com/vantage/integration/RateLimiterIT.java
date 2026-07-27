package com.vantage.integration;

import com.vantage.integration.infrastructure.NominatimGeocodingClient;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RateLimiterIT.TestSecurityConfig.class)
@Testcontainers
public class RateLimiterIT {

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
    private NominatimGeocodingClient geocodingClient;

    @Test
    void should_rate_limit_geocoding_calls_to_1_per_second_and_use_fallback() throws Exception {
        int totalCalls = 5;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Callable<NominatimGeocodingClient.Coordinates>> tasks = new ArrayList<>();
        for (int i = 0; i < totalCalls; i++) {
            tasks.add(() -> geocodingClient.geocode("test address"));
        }

        Instant start = Instant.now();
        List<Future<NominatimGeocodingClient.Coordinates>> futures = executor.invokeAll(tasks);
        List<NominatimGeocodingClient.Coordinates> results = new ArrayList<>();
        for (Future<NominatimGeocodingClient.Coordinates> f : futures) {
            results.add(f.get());
        }
        executor.shutdown();
        Duration elapsed = Duration.between(start, Instant.now());

        // With rate limiter: limitForPeriod=1, limitRefreshPeriod=1s, we expect at most 1 successful call per second.
        // The fallback returns (0,0) so we count how many are zero.
        long zeroCount = results.stream().filter(c -> c.lat() == 0.0 && c.lon() == 0.0).count();

        // At least 4 calls should fallback (since only 1 per second, and we made 5 rapidly)
        // This test will fail until RateLimiter is applied.
        assertThat(zeroCount).isGreaterThanOrEqualTo(4);
        // Elapsed time should be at least ~1 second (to allow one successful call)
        assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(1000);
    }
}
