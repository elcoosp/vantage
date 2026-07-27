package com.vantage.vendor.ui;

import com.vantage.vendor.app.VendorRegistrationResult;
import com.vantage.vendor.app.VendorService;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import com.vantage.api.api.ApiApi;
import com.vantage.api.model.AuthResponse;
import com.vantage.api.model.VendorRegistrationRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController implements ApiApi {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody VendorRegistrationRequest request) {
        VendorRegistrationResult result = vendorService.register(request);
        AuthResponse response = new AuthResponse(result.token(), result.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<AuthResponse> apiV1VendorsRegisterPost(VendorRegistrationRequest vendorRegistrationRequest) {
        com.vantage.vendor.ui.dto.VendorRegistrationRequest internalRequest =
            new com.vantage.vendor.ui.dto.VendorRegistrationRequest(
                vendorRegistrationRequest.getEmail(),
                vendorRegistrationRequest.getPassword(),
                vendorRegistrationRequest.getStoreName()
            );
        com.vantage.vendor.app.VendorRegistrationResult result = vendorService.register(internalRequest);
        AuthResponse response = new AuthResponse()
            .accessToken(result.token())
            .tenantId(result.tenantId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}