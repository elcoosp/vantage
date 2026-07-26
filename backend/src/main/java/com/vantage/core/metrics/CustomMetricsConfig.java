// backend/src/main/java/com/vantage/core/metrics/CustomMetricsConfig.java
package com.vantage.core.metrics;

import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

    public CustomMetricsConfig(MeterRegistry meterRegistry, OutboxRepository outboxRepository) {
        Gauge.builder("vantage_outbox_pending_events", outboxRepository, repo -> repo.findByStatus(OutboxStatus.PENDING).size())
                .description("Number of pending outbox events")
                .register(meterRegistry);
    }
}
