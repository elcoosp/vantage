package com.vantage.payment.infrastructure;

import com.vantage.core.admin.ChaosMonkeyService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockPaymentGatewayClientTest {

    @Mock
    private ChaosMonkeyService chaosMonkeyService;

    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private MockPaymentGatewayClient client;

    @BeforeEach
    void setUp() {
        client = new MockPaymentGatewayClient(chaosMonkeyService, meterRegistry);
    }

    @Test
    void should_throw_PaymentGatewayException_when_payment_failure_enabled() {
        when(chaosMonkeyService.isPaymentFailureEnabled()).thenReturn(true);
        assertThatThrownBy(() -> client.processPayment(UUID.randomUUID()))
            .isInstanceOf(PaymentGatewayException.class)
            .hasMessageContaining("Simulated payment gateway timeout");
    }

    @Test
    void should_return_SUCCESS_when_payment_failure_disabled() {
        when(chaosMonkeyService.isPaymentFailureEnabled()).thenReturn(false);
        PaymentResult result = client.processPayment(UUID.randomUUID());
        assertThat(result).isEqualTo(PaymentResult.SUCCESS);
    }
}
