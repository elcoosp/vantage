package com.vantage.core.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.audit.domain.EntityEvent;
import com.vantage.core.audit.domain.EntityEventRepository;
import com.vantage.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;

@Component
public class AuditHelper {

    private static final Logger log = LoggerFactory.getLogger(AuditHelper.class);
    private static EntityEventRepository entityEventRepository;
    private static ObjectMapper objectMapper;

    private final EntityEventRepository repository;
    private final ObjectMapper mapper;

    public AuditHelper(EntityEventRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        entityEventRepository = this.repository;
        objectMapper = this.mapper;
        log.info("AuditHelper initialized with repository and object mapper");
    }

    public static void captureInsert(Order order) {
        if (entityEventRepository == null) {
            log.warn("AuditHelper not initialized - skipping audit for order {}", order.getId());
            return;
        }
        createAuditEvent(order, "ORDER_CREATED", serialize(order));
    }

    public static void captureUpdate(Order order) {
        if (entityEventRepository == null) {
            log.warn("AuditHelper not initialized - skipping audit for order {}", order.getId());
            return;
        }
        createAuditEvent(order, "ORDER_UPDATED", serialize(order));
    }

    private static void createAuditEvent(Order order, String eventType, String payload) {
        EntityEvent event = new EntityEvent();
        event.setTenantId(order.getTenantId());
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        entityEventRepository.save(event);
        log.info("Saved audit event {} for order {}", eventType, order.getId());
    }

    private static String serialize(Order order) {
        if (objectMapper == null) {
            log.warn("ObjectMapper not initialized, using fallback serialization");
            return "{\"id\":\"" + order.getId() + "\",\"productId\":\"" + order.getProductId() +
                   "\",\"quantity\":" + order.getQuantity() + ",\"status\":\"" + order.getStatus().name() + "\"}";
        }
        try {
            Map<String, Object> map = Map.of(
                "id", order.getId().toString(),
                "productId", order.getProductId().toString(),
                "quantity", order.getQuantity(),
                "status", order.getStatus().name()
            );
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order", e);
            return "{}";
        }
    }
}
