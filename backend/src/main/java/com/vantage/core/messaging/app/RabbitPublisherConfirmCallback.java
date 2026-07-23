// backend/src/main/java/com/vantage/core/messaging/app/RabbitPublisherConfirmCallback.java
package com.vantage.core.messaging.app;

import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class RabbitPublisherConfirmCallback implements RabbitTemplate.ConfirmCallback {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublisherConfirmCallback.class);
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public RabbitPublisherConfirmCallback(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    @Transactional
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            return;
        }
        UUID eventId = UUID.fromString(correlationData.getId());
        outboxRepository.findById(eventId).ifPresent(event -> {
            if (ack) {
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
            } else {
                log.warn("Message NACKed for event {}: {}", eventId, cause);
            }
        });
    }
}
