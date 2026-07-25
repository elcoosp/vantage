package com.vantage.integration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByTenantIdAndRevokedFalse(UUID tenantId);

    List<ApiKey> findByKeyPrefixAndRevokedFalse(String keyPrefix);

}