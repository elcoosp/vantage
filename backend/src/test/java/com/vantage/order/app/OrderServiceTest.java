package com.vantage.order.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.events.OrderStatusChangedPayload;
import com.vantage.core.exception.ResourceNotFoundException;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.tenant.TenantContext;
import com.vantage.order.domain.Order;
import com.vantage.order.domain.OrderRepository;
import com.vantage.order.domain.OrderStatus;
import com.vantage.order.query.domain.OrderSearchView;
import com.vantage.order.query.domain.OrderSearchViewRepository;
import com.vantage.order.ui.dto.OrderResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OrderSearchViewRepository orderSearchViewRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderService orderService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxRepository, objectMapper,
                applicationEventPublisher, new SimpleMeterRegistry(), orderSearchViewRepository);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Order paidOrder(UUID orderId) {
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setProductId(UUID.randomUUID());
        order.setQuantity(2);
        order.setStatus(OrderStatus.PAID);
        return order;
    }

    @Test
    void shipOrder_should_transition_PAID_to_SHIPPED_and_emit_outbox_event() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        OrderSearchView view = new OrderSearchView();
        view.setProductName("Premium Wireless Headphones");
        when(orderSearchViewRepository.findById(orderId)).thenReturn(Optional.of(view));

        OrderResponse response = orderService.shipOrder(orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("OrderShippedEvent");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains("SHIPPED");
    }

    @Test
    void deliverOrder_should_transition_SHIPPED_to_DELIVERED() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderSearchViewRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.deliverOrder(orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shipOrder_should_reject_order_that_is_not_paid() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected PAID");
    }

    @Test
    void deliverOrder_should_reject_order_that_is_not_shipped() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deliverOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected SHIPPED");
    }

    @Test
    void getOrderForCurrentTenant_should_throw_when_order_belongs_to_other_tenant() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        order.setTenantId(UUID.randomUUID()); // different tenant
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderForCurrentTenant(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrder_should_cancel_CREATED_order_and_emit_outbox_event() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId);
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderSearchViewRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.cancelOrder(orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void cancelOrder_should_reject_PAID_order() {
        UUID orderId = UUID.randomUUID();
        Order order = paidOrder(orderId); // status PAID
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only CREATED orders");
    }
}
