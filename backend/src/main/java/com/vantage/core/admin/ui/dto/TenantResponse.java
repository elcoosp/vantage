package com.vantage.core.admin.ui.dto;

import com.vantage.vendor.domain.VendorStatus;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String storeName,
    String tenantSlug,
    String email,
    VendorStatus status
) {
    public static TenantResponse from(com.vantage.vendor.domain.Vendor vendor) {
        return new TenantResponse(
            vendor.getId(),
            vendor.getCompanyName(),
            vendor.getTenantId().toString(), // using tenantId as slug for now
            vendor.getEmail(),
            vendor.getStatus()
        );
    }
}
