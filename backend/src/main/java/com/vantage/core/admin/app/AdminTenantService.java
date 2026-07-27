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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@Service
public class AdminTenantService {
    private static final Logger log = LoggerFactory.getLogger(AdminTenantService.class);


    private final VendorRepository vendorRepository;

    public AdminTenantService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    private void ensureAdmin() {
        UUID currentTenant = TenantContext.getTenantId();
        if (currentTenant == null) {
            throw new IllegalStateException("No tenant context");
        }
        // Use findByTenantIdWithoutFilter to bypass tenant filter
        Vendor admin = vendorRepository.findByTenantIdWithoutFilter(currentTenant)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!admin.isAdmin()) {
            throw new SecurityException("Only admin users can access this endpoint");
        }
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        ensureAdmin();
        // Use findAllVendors to bypass tenant filter
        List<Vendor> vendors = vendorRepository.findAllVendors();
        return vendors.stream()
                .map(TenantResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(UUID tenantId, VendorStatus newStatus) {
        ensureAdmin();
        // Use findByTenantIdWithoutFilter to bypass tenant filter
        Vendor vendor = vendorRepository.findByTenantIdWithoutFilter(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found for tenant: " + tenantId));
        vendor.setStatus(newStatus);
        vendorRepository.save(vendor);
    }
}
