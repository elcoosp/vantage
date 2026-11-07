package com.vantage.order.query.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventId;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.core.events.OrderCreatedPayload;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import com.vantage.core.events.OrderStatusChangedPayload;
import com.vantage.core.events.PaymentFailedPayload;
import com.vantage.core.events.PaymentSucceededPayload;
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

            if (processedEventRepository.existsById(new ProcessedEventId(eventId, "order-search-projector"))) {
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
            if (orderSearchViewRepository.existsById(eventPayload.orderId())) {
                // A lifecycle event (cancel/ship) already created the row while
                // this CREATE projection was in flight — do not regress status.
                log.info("Search view row for {} already exists; skipping CREATE projection", eventPayload.orderId());
            } else {
                orderSearchViewRepository.save(view);
            }

            ProcessedEvent processedEvent = new ProcessedEvent(
                    new ProcessedEventId(eventId, "order-search-projector"),
                    eventPayload.tenantId(),
                    Instant.now());
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
            if (processedEventRepository.existsById(new ProcessedEventId(eventId, "order-search-projector"))) {
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
            // Stale-event guard: redelivered payment events (retry after an
            // initial failure) must not regress fulfillment progress — a PAID
            // event arriving after SHIPPED/DELIVERED/CANCELLED is a no-op.
            String current = view.getStatus();
            boolean stale = "SHIPPED".equals(current) || "DELIVERED".equals(current)
                    || "CANCELLED".equals(current)
                    || ("PAID".equals(current) && "PAID".equals(newStatus));
            if (!stale) {
                view.setStatus(newStatus);
                orderSearchViewRepository.save(view);
                log.info("Projected {} {} to search view with status {}", routingKey, eventId, newStatus);
            } else {
                log.info("Skipping stale {} for order {} (search view already {})", routingKey, orderId, current);
            }

            ProcessedEvent processedEvent = new ProcessedEvent(
                    new ProcessedEventId(eventId, "order-search-projector"),
                    tenantId,
                    Instant.now());
            processedEventRepository.save(processedEvent);
        } catch (Exception e) {
            log.error("Error processing payment event {}", eventId, e);
            throw new IllegalStateException("Failed to process payment event", e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Keeps the search projection in sync with fulfillment progress
     * (PAID → SHIPPED → DELIVERED) emitted by {@code OrderService}.
     */
    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.order.lifecycle.cqrs", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = {"OrderShippedEvent", "OrderDeliveredEvent", "OrderCancelledEvent"}
        )
    })
    @Transactional
    public void handleOrderLifecycleEvent(@Payload String payload,
                                          @Header("eventId") String eventIdHeader,
                                          @Header("amqp_receivedRoutingKey") String routingKey) {
        UUID eventId = UUID.fromString(eventIdHeader);

        try {
            if (processedEventRepository.existsById(new ProcessedEventId(eventId, "order-search-projector"))) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            OrderStatusChangedPayload eventPayload = objectMapper.readValue(payload, OrderStatusChangedPayload.class);
            UUID orderId = eventPayload.orderId();
            UUID tenantId = eventPayload.tenantId();

            TenantContext.setTenantId(tenantId);

            orderSearchViewRepository.findById(orderId).ifPresentOrElse(view -> {
                view.setStatus(eventPayload.newStatus());
                orderSearchViewRepository.save(view);
                log.info("Projected {} {} to search view with status {}", routingKey, eventId, eventPayload.newStatus());
            }, () -> {
                // The CREATE projection may still be in flight when a lifecycle
                // event arrives (e.g. the vendor cancelled within the outbox
                // poll interval) — upsert instead of dropping the event.
                OrderSearchView created = new OrderSearchView();
                created.setOrderId(orderId);
                created.setTenantId(tenantId);
                created.setProductName(eventPayload.productName() == null ? "" : eventPayload.productName());
                created.setStatus(eventPayload.newStatus());
                created.setQuantity(eventPayload.quantity() == null ? 0 : eventPayload.quantity());
                created.setCreatedAt(Instant.now());
                orderSearchViewRepository.save(created);
                log.info("Created search view row for {} with status {} (out-of-order arrival)", orderId, eventPayload.newStatus());
            });

            ProcessedEvent processedEvent = new ProcessedEvent(
                    new ProcessedEventId(eventId, "order-search-projector"),
                    tenantId,
                    Instant.now());
            processedEventRepository.save(processedEvent);
        } catch (Exception e) {
            log.error("Error processing order lifecycle event {}", eventId, e);
            throw new IllegalStateException("Failed to process order lifecycle event", e);
        } finally {
            TenantContext.clear();
        }
    }
}
