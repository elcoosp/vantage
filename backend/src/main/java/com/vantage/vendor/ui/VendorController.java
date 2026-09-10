package com.vantage.vendor.ui;

import com.vantage.core.security.JwtService;
import com.vantage.vendor.app.VendorRegistrationResult;
import com.vantage.vendor.app.VendorService;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.LoginRequest;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VendorRepository vendorRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public VendorController(VendorService vendorService,
                            VendorRepository vendorRepository,
                            JwtService jwtService,
                            PasswordEncoder passwordEncoder) {
        this.vendorService = vendorService;
        this.vendorRepository = vendorRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody VendorRegistrationRequest request) {
        VendorRegistrationResult result = vendorService.register(request);
        AuthResponse response = new AuthResponse(result.token(), result.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Vendor vendor = vendorRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), vendor.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(vendor.getTenantId());
        return ResponseEntity.ok(new AuthResponse(token, vendor.getTenantId()));
    }
}
