package com.vantage.order.query.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OrderSearchViewRepository extends JpaRepository<OrderSearchView, UUID> {
    Page<OrderSearchView> findByTenantId(UUID tenantId, Pageable pageable);
    Page<OrderSearchView> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
}
