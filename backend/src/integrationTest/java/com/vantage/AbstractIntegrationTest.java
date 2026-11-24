package com.vantage;

import com.vantage.VantageApplication;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Shared base for Docker-dependent integration tests.
 *
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = VantageApplication.class)
 * Postgres + RabbitMQ Testcontainers, which made the full suite take ~22 minutes. The containers
 * are now declared as {@code static} fields started exactly once per JVM (Gradle runs all
 * integration tests in a single worker JVM), so the whole suite reuses one Postgres and one
 * RabbitMQ instance. Each subclass still contributes its own extra {@code @DynamicPropertySource}
 * entries on top of {@link #baseProperties(DynamicPropertyRegistry)}.</p>
 *
 * <p>Spring Boot 4 no longer auto-configures a {@link RestClient} bean, so it is declared here
 * with the random server port injected via {@link LocalServerPort}.</p>
 */
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES;
    protected static final RabbitMQContainer RABBITMQ;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");
        POSTGRES.start();
        RABBITMQ.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                RABBITMQ.stop();
            } finally {
                POSTGRES.stop();
            }
        }));
    }

    protected static void baseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.primary.username", POSTGRES::getUsername);
        registry.add("spring.datasource.primary.password", POSTGRES::getPassword);
        registry.add("spring.datasource.replica.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.replica.username", POSTGRES::getUsername);
        registry.add("spring.datasource.replica.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        // Keep the Hikari pool tiny: tests run sequentially (one context at a time) and only need
        // a handful of connections. A large pool across 30 contexts against one shared Postgres
        // exhausts its connection limit and destabilizes the shared container.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
    }

    @Configuration
    public static class TestRestClientConfig {
        @Bean
        @Lazy
        public RestClient restClient(@LocalServerPort int port, RestClient.Builder builder) {
            return builder.baseUrl("http://localhost:" + port).build();
        }
    }
}