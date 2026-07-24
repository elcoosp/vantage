package com.vantage.integration.ui;

import com.vantage.core.tenant.TenantContext;
import com.vantage.integration.ui.dto.WebhookUpdateRequest;
import com.vantage.integration.ui.dto.WebhookUpdateResponse;
import com.vantage.vendor.app.VendorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final VendorService vendorService;

    public WebhookController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PutMapping
    public WebhookUpdateResponse updateWebhook(@Valid @RequestBody WebhookUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        String secret = vendorService.updateWebhookUrl(tenantId, request.webhookUrl());
        return new WebhookUpdateResponse(secret);
    }
}
