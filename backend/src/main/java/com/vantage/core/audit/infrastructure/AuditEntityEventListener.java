package com.vantage.core.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.audit.domain.EntityEvent;
import com.vantage.core.audit.domain.EntityEventRepository;
import com.vantage.order.domain.Order;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public class AuditEntityEventListener implements PostInsertEventListener, PostUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEntityEventListener.class);
    private final EntityEventRepository entityEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEntityEventListener(EntityEventRepository entityEventRepository, ObjectMapper objectMapper) {
        this.entityEventRepository = entityEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!(event.getEntity() instanceof Order order)) {
            return;
        }
        log.info("PostInsert event for Order: {}", order.getId());
        createAuditEvent(order, "ORDER_CREATED", serialize(order));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!(event.getEntity() instanceof Order order)) {
            return;
        }
        if (!isStatusChanged(event)) {
            log.debug("Order {} status did not change, skipping audit", order.getId());
            return;
        }
        log.info("PostUpdate event for Order: {} status changed", order.getId());
        createAuditEvent(order, "ORDER_UPDATED", serialize(order));
    }

    private boolean isStatusChanged(PostUpdateEvent event) {
        String[] propertyNames = event.getPersister().getPropertyNames();
        int statusIndex = -1;
        for (int i = 0; i < propertyNames.length; i++) {
            if ("status".equals(propertyNames[i])) {
                statusIndex = i;
                break;
            }
        }
        if (statusIndex == -1) {
            return false;
        }
        Object oldStatus = event.getOldState()[statusIndex];
        Object newStatus = event.getState()[statusIndex];
        return !Objects.equals(oldStatus, newStatus);
    }

    private void createAuditEvent(Order order, String eventType, String payload) {
        EntityEvent entityEvent = new EntityEvent();
        entityEvent.setTenantId(order.getTenantId());
        entityEvent.setAggregateType("ORDER");
        entityEvent.setAggregateId(order.getId());
        entityEvent.setEventType(eventType);
        entityEvent.setPayload(payload);
        entityEvent.setCreatedAt(Instant.now());
        entityEventRepository.save(entityEvent);
        log.info("Saved audit event {} for Order {}", eventType, order.getId());
    }

    private String serialize(Order order) {
        try {
            Map<String, Object> map = Map.of(
                "id", order.getId().toString(),
                "productId", order.getProductId().toString(),
                "quantity", order.getQuantity(),
                "status", order.getStatus().name()
            );
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Order {} for audit", order.getId(), e);
            return "{}";
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}
