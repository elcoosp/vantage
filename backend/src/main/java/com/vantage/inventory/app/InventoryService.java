package com.vantage.inventory.app;

import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryService {
    public InventoryResponse updateInventory(UUID productId, Long ifMatch, InventoryUpdateRequest request) {
        return null;
    }
}
