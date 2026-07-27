package com.vantage.core.admin;

import com.vantage.core.admin.ChaosMonkeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class ChaosMonkeyServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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
    }

    @Autowired
    private ChaosMonkeyService chaosMonkeyService;

    @Test
    void should_enable_and_disable_payment_failure_flag() {
        assertThat(chaosMonkeyService.isPaymentFailureEnabled()).isFalse();

        chaosMonkeyService.enablePaymentFailure();
        assertThat(chaosMonkeyService.isPaymentFailureEnabled()).isTrue();

        chaosMonkeyService.disablePaymentFailure();
        assertThat(chaosMonkeyService.isPaymentFailureEnabled()).isFalse();
    }
}
