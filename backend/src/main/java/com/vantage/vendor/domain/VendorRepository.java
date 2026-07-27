package com.vantage.vendor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByEmail(String email);
    Optional<Vendor> findByTenantId(UUID tenantId);

    @Query(value = "SELECT * FROM vendors", nativeQuery = true)
    List<Vendor> findAllVendors();

    @Query(value = "SELECT * FROM vendors WHERE id = :id", nativeQuery = true)
    Optional<Vendor> findByIdWithoutFilter(@Param("id") UUID id);
}
