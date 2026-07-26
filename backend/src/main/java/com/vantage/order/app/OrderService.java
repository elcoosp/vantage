// backend/src/main/java/com/vantage/order/app/OrderService.java
package com.vantage.order.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.events.OrderCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.vantage.order.app.event.OrderCreatedPayload;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.order.ui.dto.OrderRequest;
import com.vantage.order.ui.dto.OrderResponse;
import com.vantage.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.annotation.NewSpan;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
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
}