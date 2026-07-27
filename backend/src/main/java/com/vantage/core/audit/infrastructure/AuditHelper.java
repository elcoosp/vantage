package com.vantage.core.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.audit.domain.EntityEvent;
import com.vantage.core.audit.domain.EntityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;

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

    public static void captureEvent(String aggregateType, UUID aggregateId, String eventType, Object payload, UUID tenantId) {
        if (entityEventRepository == null) {
            log.warn("AuditHelper not initialized - skipping audit for aggregate {}", aggregateId);
            return;
        }
        String jsonPayload = serialize(payload);
        EntityEvent event = new EntityEvent();
        event.setTenantId(tenantId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(jsonPayload);
        event.setCreatedAt(Instant.now());
        entityEventRepository.save(event);
        log.info("Saved audit event {} for aggregate {}", eventType, aggregateId);
    }

    private static String serialize(Object obj) {
        if (objectMapper == null) {
            log.warn("ObjectMapper not initialized, using fallback serialization");
            return "{"error":"objectMapper not available"}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object", e);
            return "{"error":"serialization failed"}";
        }
    }
}
