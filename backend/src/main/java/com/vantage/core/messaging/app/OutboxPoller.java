// backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java
package com.vantage.core.messaging.app;

import org.springframework.transaction.annotation.Transactional;

import com.vantage.core.db.DistributedLockService;

import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(name = "vantage.outbox.enabled", havingValue = "true")
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxRepository outboxRepository;
    private final DistributedLockService distributedLockService;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, TransactionTemplate transactionTemplate, DistributedLockService distributedLockService) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = transactionTemplate;
        this.distributedLockService = distributedLockService;
        this.rabbitTemplate.setConfirmCallback(this::confirm);
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublish() {
        String lockName = "outbox_poller";
        if (!distributedLockService.tryAcquireLock(lockName)) {
            log.info("Another instance holds the lock. Skipping run.");
            return;
        }
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxStatus.PENDING);
            for (OutboxEvent event : pendingEvents) {
                CorrelationData correlationData = new CorrelationData(event.getId().toString());
                Message message = MessageBuilder
                        .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("eventId", event.getId().toString())
                        .build();
                log.info("Publishing outbox event {} to RabbitMQ", event.getId());
                rabbitTemplate.send(RabbitMQConfig.EXCHANGE, event.getEventType(), message, correlationData);
            }
        } finally {
            distributedLockService.releaseLock(lockName);
        }
    }

    private void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null || !ack) {
            log.warn("Received NACK or null correlation data for publisher confirm. Cause: {}", cause);
            return;
        }
        UUID eventId = UUID.fromString(correlationData.getId());
        transactionTemplate.executeWithoutResult(status -> {
            outboxRepository.findById(eventId).ifPresent(event -> {
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
                log.info("Successfully marked outbox event {} as PUBLISHED", eventId);
            });
        });
    }
}