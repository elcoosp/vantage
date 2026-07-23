package com.vantage.vendor.ui.dto;

import java.util.UUID;

public record AuthResponse(String token, UUID tenantId) {
}
