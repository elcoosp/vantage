package com.vantage.vendor.app;

import com.vantage.core.security.JwtService;
import com.vantage.core.tenant.TenantContext;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public VendorService(VendorRepository vendorRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(VendorRegistrationRequest request) {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        try {
            Vendor vendor = new Vendor();
            vendor.setEmail(request.email());
            vendor.setPasswordHash(passwordEncoder.encode(request.password()));
            vendor.setCompanyName(request.name());
            vendorRepository.save(vendor);
            String token = jwtService.generateToken(tenantId);
            return new AuthResponse(token, tenantId);
        } finally {
            TenantContext.clear();
        }
    }
}
