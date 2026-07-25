package com.vantage.order.query.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.app.event.OrderCreatedPayload;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import com.vantage.payment.app.event.PaymentFailedPayload;
import com.vantage.payment.app.event.PaymentSucceededPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Component
public class OrderSearchProjector {
    private static final Logger log = LoggerFactory.getLogger(OrderSearchProjector.class);

    private final OrderSearchViewRepository orderSearchViewRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public OrderSearchProjector(OrderSearchViewRepository orderSearchViewRepository,
                                ProcessedEventRepository processedEventRepository,
                                ObjectMapper objectMapper) {
        this.orderSearchViewRepository = orderSearchViewRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.order.events.cqrs", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = "OrderCreatedEvent"
        )
    })
    @Transactional
    public void handleOrderCreatedEvent(@Payload String payload, @Header("eventId") String eventIdHeader) {
        UUID eventId = UUID.fromString(eventIdHeader);

        try {
            OrderCreatedPayload eventPayload = objectMapper.readValue(payload, OrderCreatedPayload.class);
            TenantContext.setTenantId(eventPayload.tenantId());

            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            OrderSearchView view = new OrderSearchView();
            view.setOrderId(eventPayload.orderId());
            view.setTenantId(eventPayload.tenantId());
            view.setProductName(eventPayload.productName());
            view.setStatus("CREATED");
            view.setQuantity(eventPayload.quantity());
            view.setCreatedAt(Instant.now());
            orderSearchViewRepository.save(view);

            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventId);
            processedEvent.setTenantId(eventPayload.tenantId());
            processedEvent.setProcessedAt(Instant.now());
            processedEventRepository.save(processedEvent);

            log.info("Projected OrderCreatedEvent {} to search view", eventId);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent {}", eventId, e);
            throw new IllegalStateException("Failed to process OrderCreatedEvent", e);
        } finally {
            TenantContext.clear();
        }
    }

    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.payment.events.cqrs", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = {"PaymentSucceededEvent", "PaymentFailedEvent"}
        )
    })
    @Transactional
    public void handlePaymentEvent(@Payload String payload,
                                   @Header("eventId") String eventIdHeader,
                                   @Header("amqp_receivedRoutingKey") String routingKey) {
        UUID eventId = UUID.fromString(eventIdHeader);

        try {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            UUID orderId;
            UUID tenantId;
            String newStatus;

            if ("PaymentSucceededEvent".equals(routingKey)) {
                PaymentSucceededPayload eventPayload = objectMapper.readValue(payload, PaymentSucceededPayload.class);
                orderId = eventPayload.orderId();
                tenantId = eventPayload.tenantId();
                newStatus = "PAID";
            } else if ("PaymentFailedEvent".equals(routingKey)) {
                PaymentFailedPayload eventPayload = objectMapper.readValue(payload, PaymentFailedPayload.class);
                orderId = eventPayload.orderId();
                tenantId = eventPayload.tenantId();
                newStatus = "CANCELLED";
            } else {
                log.warn("Unhandled routing key: {}", routingKey);
                return;
            }

            TenantContext.setTenantId(tenantId);

            OrderSearchView view = orderSearchViewRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found in search view: " + orderId));
            view.setStatus(newStatus);
            orderSearchViewRepository.save(view);

            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventId);
            processedEvent.setTenantId(tenantId);
            processedEvent.setProcessedAt(Instant.now());
            processedEventRepository.save(processedEvent);

            log.info("Projected {} {} to search view with status {}", routingKey, eventId, newStatus);
        } catch (Exception e) {
            log.error("Error processing payment event {}", eventId, e);
            throw new IllegalStateException("Failed to process payment event", e);
        } finally {
            TenantContext.clear();
        }
    }
}
