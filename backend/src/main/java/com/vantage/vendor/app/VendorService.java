package com.vantage.vendor.app;

import com.vantage.core.security.JwtService;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorService.class);

    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public VendorService(VendorRepository vendorRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public VendorRegistrationResult register(VendorRegistrationRequest request) {
        UUID tenantId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setTenantId(tenantId);
        vendor.setEmail(request.email());
        vendor.setPasswordHash(passwordEncoder.encode(request.password()));
        vendor.setCompanyName(request.name());

        vendorRepository.save(vendor);
        log.info("Registered new vendor with tenant ID: {}", tenantId);

        String token = jwtService.generateToken(tenantId);
        return new VendorRegistrationResult(tenantId, token);
    }
}
