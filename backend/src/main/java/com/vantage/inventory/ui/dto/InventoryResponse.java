package com.vantage.inventory.ui.dto;

import java.util.UUID;

public record InventoryResponse(UUID productId, Integer quantity, Long version) {
}
