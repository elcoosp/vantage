package com.vantage.core.admin;
import com.vantage.AbstractIntegrationTest;

import com.vantage.core.admin.ChaosMonkeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChaosMonkeyServiceTest extends AbstractIntegrationTest {


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        baseProperties(registry);
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
