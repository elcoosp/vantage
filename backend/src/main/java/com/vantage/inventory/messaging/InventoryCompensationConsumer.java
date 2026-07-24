package com.vantage.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.order.app.event.InventoryReleasedPayload;
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
@ConditionalOnProperty(name = "vantage.inventory.compensation.enabled", havingValue = "true")
public class InventoryCompensationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryCompensationConsumer.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryCompensationConsumer(InventoryRepository inventoryRepository, ProcessedEventRepository processedEventRepository, ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.inventory.events", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = "InventoryReleasedEvent"
        )
    })
    @Transactional
    public void handleInventoryReleasedEvent(@Payload String payload, @Header("eventId") String eventIdHeader) {
        InventoryReleasedPayload eventPayload;
        try {
            eventPayload = objectMapper.readValue(payload, InventoryReleasedPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize InventoryReleasedPayload", e);
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

            Inventory inventory = inventoryRepository.findByProductId(eventPayload.productId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found for product: " + eventPayload.productId()));

            inventory.setQuantity(inventory.getQuantity() + eventPayload.releasedQuantity());
            inventoryRepository.save(inventory);
        } finally {
            TenantContext.clear();
        }
    }
}
