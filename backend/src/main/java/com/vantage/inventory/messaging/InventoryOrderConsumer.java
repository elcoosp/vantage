package com.vantage.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.OutboxEvent;
import com.vantage.core.messaging.domain.OutboxRepository;
import com.vantage.core.messaging.domain.OutboxStatus;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.app.event.InventoryReservationFailedPayload;
import com.vantage.inventory.app.event.InventoryReservedPayload;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.order.app.event.OrderCreatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryOrderConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryOrderConsumer(InventoryRepository inventoryRepository, OutboxRepository outboxRepository, ProcessedEventRepository processedEventRepository, ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "vantage.order.events")
    @Transactional
    public void handleOrderCreatedEvent(@Payload OrderCreatedPayload payload, @Header("eventId") String eventIdHeader) {
        UUID eventId = UUID.fromString(eventIdHeader);
        TenantContext.setTenantId(payload.tenantId());

        try {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }

            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventId);
            processedEvent.setTenantId(payload.tenantId());
            processedEvent.setProcessedAt(Instant.now());
            processedEventRepository.save(processedEvent);

            try {
                Inventory inventory = inventoryRepository.findByProductId(payload.productId())
                        .orElseThrow(() -> new IllegalStateException("Inventory not found for product: " + payload.productId()));

                if (inventory.getQuantity() < payload.quantity()) {
                    log.warn("Insufficient stock for product {}. Requested: {}, Available: {}", payload.productId(), payload.quantity(), inventory.getQuantity());
                    emitReservationFailedEvent(payload);
                    return;
                }

                inventory.setQuantity(inventory.getQuantity() - payload.quantity());
                inventoryRepository.saveAndFlush(inventory);

                emitReservedEvent(payload);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic locking failure for product: {}", payload.productId());
                emitReservationFailedEvent(payload);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void emitReservedEvent(OrderCreatedPayload payload) {
        InventoryReservedPayload eventPayload = new InventoryReservedPayload(payload.orderId(), payload.tenantId(), payload.productId(), payload.quantity());
        saveOutboxEvent("InventoryReservedEvent", payload.orderId(), eventPayload);
    }

    private void emitReservationFailedEvent(OrderCreatedPayload payload) {
        InventoryReservationFailedPayload eventPayload = new InventoryReservationFailedPayload(payload.orderId(), payload.tenantId(), payload.productId(), payload.quantity());
        saveOutboxEvent("InventoryReservationFailedEvent", payload.orderId(), eventPayload);
    }

    private void saveOutboxEvent(String eventType, UUID aggregateId, Object eventPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("INVENTORY");
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
