package com.vantage.core.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EntityEventRepository extends JpaRepository<EntityEvent, UUID> {
    List<EntityEvent> findByAggregateIdOrderByCreatedAtAsc(UUID aggregateId);
}
