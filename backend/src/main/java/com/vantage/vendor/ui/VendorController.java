package com.vantage.vendor.ui;

import com.vantage.vendor.app.VendorService;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody VendorRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.register(request));
    }
}
