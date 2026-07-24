package com.vantage.integration.ui;

import com.vantage.integration.ui.dto.WebhookUpdateRequest;
import com.vantage.integration.ui.dto.WebhookUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @PutMapping
    public WebhookUpdateResponse updateWebhook(@Valid @RequestBody WebhookUpdateRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
