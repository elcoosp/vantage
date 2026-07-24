package com.vantage.payment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.app.event.InventoryReservedPayload;
import com.vantage.payment.app.event.PaymentFailedPayload;
import com.vantage.payment.app.event.PaymentSucceededPayload;
import com.vantage.payment.infrastructure.MockPaymentGatewayClient;
import com.vantage.payment.infrastructure.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "vantage.payment.enabled", havingValue = "true")
public class PaymentInventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentInventoryConsumer.class);

    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final MockPaymentGatewayClient mockPaymentGatewayClient;
    private final ObjectMapper objectMapper;

    public PaymentInventoryConsumer(OutboxRepository outboxRepository, ProcessedEventRepository processedEventRepository, MockPaymentGatewayClient mockPaymentGatewayClient, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.mockPaymentGatewayClient = mockPaymentGatewayClient;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "vantage.inventory.events")
    @Transactional
    public void handleInventoryReservedEvent(@Payload String payload, @Header("eventId") String eventIdHeader, @Header("amqp_receivedRoutingKey") String routingKey) {
        if (!"InventoryReservedEvent".equals(routingKey)) {
            log.info("Ignoring non-reserved event with routing key: {}", routingKey);
            return;
        }

        InventoryReservedPayload eventPayload;
        try {
            eventPayload = objectMapper.readValue(payload, InventoryReservedPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize InventoryReservedPayload", e);
            throw new IllegalStateException("Failed to deserialize payload", e);
        }

        UUID eventId = UUID.fromString(eventIdHeader);
        TenantContext.setTenantId(eventPayload.tenantId());

        try {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventId);
            processedEvent.setTenantId(eventPayload.tenantId());
            processedEvent.setProcessedAt(Instant.now());
            processedEventRepository.save(processedEvent);

            PaymentResult result = mockPaymentGatewayClient.processPayment(eventPayload.orderId());

            if (result == PaymentResult.SUCCESS) {
                emitPaymentSucceededEvent(eventPayload);
            } else {
                String reason = result == PaymentResult.CIRCUIT_OPEN ? "CIRCUIT_OPEN" : "GATEWAY_TIMEOUT";
                emitPaymentFailedEvent(eventPayload, reason);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void emitPaymentSucceededEvent(InventoryReservedPayload payload) {
        PaymentSucceededPayload eventPayload = new PaymentSucceededPayload(payload.orderId(), payload.tenantId());
        saveOutboxEvent("PaymentSucceededEvent", payload.orderId(), eventPayload);
    }

    private void emitPaymentFailedEvent(InventoryReservedPayload payload, String reason) {
        PaymentFailedPayload eventPayload = new PaymentFailedPayload(payload.orderId(), payload.tenantId(), reason);
        saveOutboxEvent("PaymentFailedEvent", payload.orderId(), eventPayload);
    }

    private void saveOutboxEvent(String eventType, UUID aggregateId, Object eventPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("PAYMENT");
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(jsonPayload);
            event.setStatus(OutboxStatus.PENDING);
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
