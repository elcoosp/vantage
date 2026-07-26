// backend/src/main/java/com/vantage/core/metrics/CustomMetricsConfig.java
package com.vantage.core.metrics;

import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class CustomMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final OutboxRepository outboxRepository;

    public CustomMetricsConfig(MeterRegistry meterRegistry, OutboxRepository outboxRepository) {
        this.meterRegistry = meterRegistry;
        this.outboxRepository = outboxRepository;

        Gauge.builder("vantage_outbox_pending_events", outboxRepository, repo -> repo.findByStatus(OutboxStatus.PENDING).size())
                .description("Number of pending outbox events")
                .register(meterRegistry);
    }

    public void incrementOrdersCreated(UUID tenantId) {
        Counter.builder("vantage_orders_created_total")
                .description("Total number of orders created")
                .tag("tenant_id", tenantId.toString())
                .register(meterRegistry)
                .increment();
    }

    public void incrementPaymentsFailed(String reason) {
        Counter.builder("vantage_payments_failed_total")
                .description("Total number of payment failures")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startPaymentGatewayTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentGatewayDuration(Timer.Sample sample) {
        sample.stop(Timer.builder("vantage_payment_gateway_duration")
                .description("Duration of mock payment gateway calls")
                .register(meterRegistry));
    }
}
