package com.vantage.product.ui.dto;

import java.util.UUID;

public record ProductResponse(UUID id, String name, String description, Double price) {
}
