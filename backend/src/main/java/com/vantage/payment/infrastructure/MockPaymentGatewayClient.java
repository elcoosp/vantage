package com.vantage.payment.infrastructure;

import com.vantage.core.admin.app.ChaosMonkeyService;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGatewayClient {
    private final ChaosMonkeyService chaosMonkeyService;
    private final MeterRegistry meterRegistry;

    public MockPaymentGatewayClient(ChaosMonkeyService chaosMonkeyService, MeterRegistry meterRegistry) {
        this.chaosMonkeyService = chaosMonkeyService;
        this.meterRegistry = meterRegistry;
    }

    private volatile boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Retry(name = "payment")
    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(UUID orderId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (chaosMonkeyService.isPaymentFailureEnabled() || simulateFailure) {
                throw new PaymentGatewayException("Simulated payment gateway timeout");
            }
            return PaymentResult.SUCCESS;
        } finally {
            sample.stop(Timer.builder("vantage_payment_gateway_duration")
                    .description("Duration of mock payment gateway calls")
                    .register(meterRegistry));
        }
    }

    private PaymentResult paymentFallback(UUID orderId, Exception e) {
        if (e instanceof CallNotPermittedException) {
            return PaymentResult.CIRCUIT_OPEN;
        }
        return PaymentResult.GATEWAY_TIMEOUT;
    }
}