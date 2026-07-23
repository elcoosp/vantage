package com.vantage.vendor.app;

import java.util.UUID;

public record VendorRegistrationResult(UUID tenantId, String token) {
}
