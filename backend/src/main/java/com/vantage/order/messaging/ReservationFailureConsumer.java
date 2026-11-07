package com.vantage.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.events.InventoryReservationFailedPayload;
import com.vantage.core.events.OrderStatusChangedPayload;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
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

import java.util.UUID;

/**
 * Saga side-arm: cancels the order when the inventory reservation fails
 * (e.g. someone ordered 999 units of an 8-unit product). Without this the
 * order would sit in CREATED forever after a failed reservation.
 */
@Component
@ConditionalOnProperty(name = "vantage.payment.enabled", havingValue = "true")
public class ReservationFailureConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservationFailureConsumer.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ReservationFailureConsumer(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(name = "vantage.order.reservation.failure.events", durable = "true"),
                    exchange = @Exchange(name = "vantage.events", type = "direct"),
                    key = "InventoryReservationFailedEvent"
            )
    })
    @Transactional
    public void handleReservationFailure(@Payload String payload, @Header("eventId") String eventIdHeader) {
        try {
            InventoryReservationFailedPayload eventPayload =
                    objectMapper.readValue(payload, InventoryReservationFailedPayload.class);
            TenantContext.setTenantId(eventPayload.tenantId());

            Order order = orderRepository.findById(eventPayload.orderId()).orElse(null);
            if (order == null) {
                log.warn("Order {} not found for reservation failure", eventPayload.orderId());
                return;
            }
            if (order.getStatus() != OrderStatus.CREATED) {
                // The saga already moved on (payment captured before the
                // reservation failed) — the compensation consumer handles stock.
                log.info("Order {} in status {}, ignoring reservation failure", order.getId(), order.getStatus());
                return;
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            OrderStatusChangedPayload cancelPayload = new OrderStatusChangedPayload(
                    order.getId(), order.getTenantId(), OrderStatus.CREATED.name(), OrderStatus.CANCELLED.name(),
                    null, order.getQuantity());
            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("ORDER");
            event.setAggregateId(order.getId());
            event.setEventType("OrderCancelledEvent");
            event.setPayload(objectMapper.writeValueAsString(cancelPayload));
            event.setStatus(OutboxStatus.PENDING);
            outboxRepository.save(event);

            log.info("Order {} cancelled due to inventory reservation failure", order.getId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize InventoryReservationFailedPayload", e);
        } finally {
            TenantContext.clear();
        }
    }
}
