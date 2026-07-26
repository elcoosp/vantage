package com.vantage.core.audit.ui;

import com.vantage.core.audit.domain.EntityEvent;
import com.vantage.core.audit.domain.EntityEventRepository;
import com.vantage.core.audit.ui.dto.AuditEventResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final EntityEventRepository entityEventRepository;

    public AuditController(EntityEventRepository entityEventRepository) {
        this.entityEventRepository = entityEventRepository;
    }

    @GetMapping("/orders/{orderId}")
    public List<AuditEventResponse> getOrderTimeline(@PathVariable UUID orderId) {
        List<EntityEvent> events = entityEventRepository.findByAggregateIdOrderByCreatedAtAsc(orderId);
        return events.stream()
            .map(e -> new AuditEventResponse(
                e.getId(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getEventType(),
                e.getPayload(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }
}
