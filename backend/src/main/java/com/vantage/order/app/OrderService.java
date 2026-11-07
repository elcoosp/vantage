// backend/src/main/java/com/vantage/order/app/OrderService.java
package com.vantage.order.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.events.OrderCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.vantage.core.events.OrderCreatedPayload;
import com.vantage.core.events.OrderStatusChangedPayload;
import com.vantage.core.exception.ResourceNotFoundException;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.annotation.NewSpan;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final OrderSearchViewRepository orderSearchViewRepository;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher, MeterRegistry meterRegistry, OrderSearchViewRepository orderSearchViewRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.orderSearchViewRepository = orderSearchViewRepository;
    }

    @Transactional
    @NewSpan("order.create")
    public OrderResponse createOrder(OrderRequest request) {
        Order order = new Order();
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setStatus(OrderStatus.CREATED);
        orderRepository.save(order);

        meterRegistry.counter("vantage_orders_created_total", "tenant_id", order.getTenantId().toString()).increment();

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getProductId(), TenantContext.getTenantId()));
        // Publish internal event for cache eviction and other listeners

        OrderCreatedPayload payload = new OrderCreatedPayload(order.getId(), TenantContext.getTenantId(), order.getProductId(), request.productName(), order.getQuantity());
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new OrderSerializationException("Failed to serialize OrderCreatedPayload for order " + order.getId());
        }

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType("OrderCreatedEvent");
        event.setPayload(jsonPayload);
        event.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(event);

        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(), order.getStatus());
    }

    /**
     * Loads an order owned by the current tenant, throwing 404 semantics when
     * it does not exist or belongs to another tenant.
     */
    @Transactional(readOnly = true)
    public Order getOrderForCurrentTenant(UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getTenantId().equals(TenantContext.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    /**
     * Advances a paid order to SHIPPED (fulfillment hand-off).
     *
     * <p>Publishes an {@code OrderShippedEvent} through both the in-process
     * event bus and the transactional outbox so the CQRS search projection and
     * the live ops map stay consistent with the authoritative order row.</p>
     */
    @Transactional
    @NewSpan("order.ship")
    public OrderResponse shipOrder(UUID orderId) {
        return transition(orderId, OrderStatus.PAID, OrderStatus.SHIPPED, "OrderShippedEvent");
    }

    /**
     * Marks a shipped order as DELIVERED (customer confirmation).
     *
     * <p>Publishes an {@code OrderDeliveredEvent} through the same channels.</p>
     */
    @Transactional
    @NewSpan("order.deliver")
    public OrderResponse deliverOrder(UUID orderId) {
        return transition(orderId, OrderStatus.SHIPPED, OrderStatus.DELIVERED, "OrderDeliveredEvent");
    }

    /**
     * Cancels an order that has not been captured yet.
     *
     * <p>Only CREATED orders can be cancelled: once the saga has captured
     * payment (PAID) the order must follow the fulfillment path instead.
     * Cancellation may race with the inventory reservation; the compensation
     * consumer restores any reservation that failed payment.</p>
     */
    @Transactional
    @NewSpan("order.cancel")
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = getOrderForCurrentTenant(orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot cancel order " + orderId + " in status " + order.getStatus()
                            + " (only CREATED orders can be cancelled before payment) ");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        meterRegistry.counter("vantage_orders_cancelled_total",
                "tenant_id", order.getTenantId().toString()).increment();

        // Read-your-writes for the CQRS projection (see transition()).
        orderSearchViewRepository.findById(orderId).ifPresent(view -> {
            view.setStatus(OrderStatus.CANCELLED.name());
            orderSearchViewRepository.save(view);
        });

        OrderStatusChangedPayload payload = new OrderStatusChangedPayload(
                order.getId(), order.getTenantId(), OrderStatus.CREATED.name(), OrderStatus.CANCELLED.name(),
                orderSearchViewRepository.findById(orderId).map(v -> v.getProductName()).orElse(null),
                order.getQuantity());
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new OrderSerializationException("Failed to serialize OrderCancelledEvent for order " + order.getId());
        }

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType("OrderCancelledEvent");
        event.setPayload(jsonPayload);
        event.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(event);

        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(), order.getStatus());
    }

    private OrderResponse transition(UUID orderId, OrderStatus expected, OrderStatus target, String eventType) {
        Order order = getOrderForCurrentTenant(orderId);
        if (order.getStatus() != expected) {
            throw new IllegalStateException(
                    "Cannot transition order " + orderId + " from " + order.getStatus() + " to " + target
                            + " (expected " + expected + ")");
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(target);
        orderRepository.save(order);

        meterRegistry.counter("vantage_orders_" + target.name().toLowerCase() + "_total",
                "tenant_id", order.getTenantId().toString()).increment();

        String productName = orderSearchViewRepository.findById(orderId)
                .map(view -> view.getProductName())
                .orElse(order.getProductId().toString());

        // Read-your-writes: update the CQRS search projection in the same
        // transaction so the UI reflects the transition immediately. The
        // OrderShippedEvent/OrderDeliveredEvent below keeps other instances
        // (and the ops map) in sync; the projector is idempotent.
        orderSearchViewRepository.findById(orderId).ifPresent(view -> {
            view.setStatus(target.name());
            orderSearchViewRepository.save(view);
        });

        OrderStatusChangedPayload payload = new OrderStatusChangedPayload(
                order.getId(), order.getTenantId(), oldStatus.name(), target.name(), productName, order.getQuantity());
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new OrderSerializationException("Failed to serialize " + eventType + " for order " + order.getId());
        }

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType(eventType);
        event.setPayload(jsonPayload);
        event.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(event);

        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(), order.getStatus());
    }
}
