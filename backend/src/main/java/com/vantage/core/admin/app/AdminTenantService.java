package com.vantage.core.admin.app;

import com.vantage.core.tenant.TenantContext;
import com.vantage.core.exception.ResourceNotFoundException;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.domain.VendorStatus;
import com.vantage.core.admin.ui.dto.TenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminTenantService {

    private final VendorRepository vendorRepository;

    public AdminTenantService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    // TODO: Add admin authorization check
    private void ensureAdmin() {
        UUID currentTenant = TenantContext.getTenantId();
        if (currentTenant == null) {
            throw new IllegalStateException("No tenant context");
        }
        Vendor admin = vendorRepository.findByTenantId(currentTenant)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!admin.isAdmin()) {
            throw new SecurityException("Only admin users can access this endpoint");
        }
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        ensureAdmin();
        return vendorRepository.findAllVendors().stream()
                .map(TenantResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(UUID tenantId, VendorStatus newStatus) {
        ensureAdmin();
        Vendor vendor = vendorRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for tenant: " + tenantId));
        vendor.setStatus(newStatus);
        vendorRepository.save(vendor);
    }
}
