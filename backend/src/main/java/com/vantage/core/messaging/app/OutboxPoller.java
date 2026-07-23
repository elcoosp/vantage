// backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java
package com.vantage.core.messaging.app;

import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            CorrelationData correlationData = new CorrelationData(event.getId().toString());
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, message, correlationData);
        }
    }
}
