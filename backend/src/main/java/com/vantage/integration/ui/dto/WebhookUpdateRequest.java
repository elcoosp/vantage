package com.vantage.integration.ui.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record WebhookUpdateRequest(
    @NotBlank @URL String webhookUrl
) {
}
