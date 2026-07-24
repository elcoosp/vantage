package com.vantage.payment.infrastructure;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGatewayClient {

    private volatile boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Retry(name = "payment")
    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(UUID orderId) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (simulateFailure) {
            throw new PaymentGatewayException("Simulated payment gateway timeout");
        }
        return PaymentResult.SUCCESS;
    }

    private PaymentResult paymentFallback(UUID orderId, Exception e) {
        if (e instanceof CallNotPermittedException) {
            return PaymentResult.CIRCUIT_OPEN;
        }
        return PaymentResult.GATEWAY_TIMEOUT;
    }
}
