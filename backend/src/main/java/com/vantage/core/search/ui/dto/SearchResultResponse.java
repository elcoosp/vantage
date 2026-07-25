package com.vantage.core.search.ui.dto;

import java.util.UUID;

public record SearchResultResponse(String entityType, UUID id, String title, String description) {
}
