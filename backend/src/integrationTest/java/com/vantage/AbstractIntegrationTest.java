package com.vantage;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Shared base for Docker-dependent integration tests.
 *
 * <p>Before this class existed every {@code @SpringBootTest} integration test started its own
 * Postgres + RabbitMQ Testcontainers, which made the full suite take ~22 minutes. The containers
 * are now declared as {@code static} fields started exactly once per JVM (Gradle runs all
 * integration tests in a single worker JVM), so the whole suite reuses one Postgres and one
 * RabbitMQ instance. Each subclass still contributes its own extra {@code @DynamicPropertySource}
 * entries on top of {@link #baseProperties(DynamicPropertyRegistry)}.</p>
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
        // Disable the production SecurityConfig so each test's permit-all TestSecurityConfig is
        // the single filter chain (Spring Security 6.4 rejects two chains matching "any request").
        registry.add("vantage.test.security.bypass", () -> "true");
    }
}
