package com.vantage.payment.infrastructure;

import com.vantage.core.admin.ChaosMonkeyService;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayClient.class);
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
    @Bulkhead(name = "payment", fallbackMethod = "paymentFallback")
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
            log.warn("Payment fallback due to circuit open for order: {}", orderId);
            return PaymentResult.CIRCUIT_OPEN;
        }
        log.warn("Payment fallback due to exception: {}", e.getMessage(), e);
        return PaymentResult.GATEWAY_TIMEOUT;
    }
}