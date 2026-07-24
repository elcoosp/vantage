// backend/src/main/java/com/vantage/core/messaging/domain/OutboxRepository.java
package com.vantage.core.messaging.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("select e from OutboxEvent e where e.status = :status order by e.createdAt asc")
    List<OutboxEvent> findByStatus(@Param("status") OutboxStatus status);

    Optional<OutboxEvent> findByAggregateId(UUID aggregateId);
}
