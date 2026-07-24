package com.vantage.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.app.event.InventoryReleasedPayload;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.payment.app.event.PaymentFailedPayload;
import com.vantage.payment.app.event.PaymentSucceededPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
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
public class PaymentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaConsumer.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentSagaConsumer(OrderRepository orderRepository, OutboxRepository outboxRepository, ProcessedEventRepository processedEventRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.payment.events", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = {"PaymentSucceededEvent", "PaymentFailedEvent"}
        )
    })
    @Transactional
    public void handlePaymentEvent(@Payload String payload, @Header("eventId") String eventIdHeader, @Header("amqp_receivedRoutingKey") String routingKey) {
        UUID eventId = UUID.fromString(eventIdHeader);
        try {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            if ("PaymentSucceededEvent".equals(routingKey)) {
                PaymentSucceededPayload eventPayload = objectMapper.readValue(payload, PaymentSucceededPayload.class);
                TenantContext.setTenantId(eventPayload.tenantId());

                ProcessedEvent processedEvent = new ProcessedEvent();
                processedEvent.setEventId(eventId);
                processedEvent.setTenantId(eventPayload.tenantId());
                processedEvent.setProcessedAt(Instant.now());
                processedEventRepository.save(processedEvent);

                Order order = orderRepository.findById(eventPayload.orderId()).orElseThrow();
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);

            } else if ("PaymentFailedEvent".equals(routingKey)) {
                PaymentFailedPayload eventPayload = objectMapper.readValue(payload, PaymentFailedPayload.class);
                TenantContext.setTenantId(eventPayload.tenantId());

                ProcessedEvent processedEvent = new ProcessedEvent();
                processedEvent.setEventId(eventId);
                processedEvent.setTenantId(eventPayload.tenantId());
                processedEvent.setProcessedAt(Instant.now());
                processedEventRepository.save(processedEvent);

                Order order = orderRepository.findById(eventPayload.orderId()).orElseThrow();
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                InventoryReleasedPayload releasedPayload = new InventoryReleasedPayload(
                    order.getId(),
                    TenantContext.getTenantId(),
                    order.getProductId(),
                    order.getQuantity()
                );
                saveOutboxEvent("InventoryReleasedEvent", order.getId(), releasedPayload);
            }
        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw new IllegalStateException("Failed to process payment event", e);
        } finally {
            TenantContext.clear();
        }
    }

    private void saveOutboxEvent(String eventType, UUID aggregateId, Object eventPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("ORDER");
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
