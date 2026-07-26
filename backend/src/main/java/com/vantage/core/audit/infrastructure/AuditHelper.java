package com.vantage.core.audit.infrastructure;

import com.vantage.core.audit.domain.EntityEvent;
import com.vantage.core.audit.domain.EntityEventRepository;
import com.vantage.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public final class AuditHelper {

    private static final Logger log = LoggerFactory.getLogger(AuditHelper.class);
    private static EntityEventRepository entityEventRepository;

    public static void setEntityEventRepository(EntityEventRepository repo) {
        entityEventRepository = repo;
    }

    public static void captureInsert(Order order) {
        System.out.println("*** AuditHelper.captureInsert called for order " + order.getId());
        if (entityEventRepository == null) {
            log.warn("AuditHelper not initialized - skipping audit for order {}", order.getId());
            System.out.println("*** AuditHelper repository is NULL");
            return;
        }
        log.info("AuditHelper capturing INSERT for order {}", order.getId());
        System.out.println("*** AuditHelper repository is set, saving event");
        EntityEvent event = new EntityEvent();
        event.setTenantId(order.getTenantId());
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType("ORDER_CREATED");
        event.setPayload(serialize(order));
        event.setCreatedAt(Instant.now());
        entityEventRepository.save(event);
        log.info("Saved ORDER_CREATED event for order {}", order.getId());
    }

    public static void captureUpdate(Order order) {
        System.out.println("*** AuditHelper.captureUpdate called for order " + order.getId());
        if (entityEventRepository == null) {
            log.warn("AuditHelper not initialized - skipping audit for order {}", order.getId());
            System.out.println("*** AuditHelper repository is NULL");
            return;
        }
        log.info("AuditHelper capturing UPDATE for order {}", order.getId());
        System.out.println("*** AuditHelper repository is set, saving event");
        EntityEvent event = new EntityEvent();
        event.setTenantId(order.getTenantId());
        event.setAggregateType("ORDER");
        event.setAggregateId(order.getId());
        event.setEventType("ORDER_UPDATED");
        event.setPayload(serialize(order));
        event.setCreatedAt(Instant.now());
        entityEventRepository.save(event);
        log.info("Saved ORDER_UPDATED event for order {}", order.getId());
    }

    private static String serialize(Order order) {
        try {
            var map = java.util.Map.of(
                "id", order.getId().toString(),
                "productId", order.getProductId().toString(),
                "quantity", order.getQuantity(),
                "status", order.getStatus().name()
            );
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize order", e);
            return "{}";
        }
    }
}
