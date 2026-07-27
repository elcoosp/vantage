package com.vantage.storefront.domain;

import com.vantage.core.domain.BaseTenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontRepository extends JpaRepository<StorefrontConfig, UUID> {
    Optional<StorefrontConfig> findByTenantId(UUID tenantId);
}
