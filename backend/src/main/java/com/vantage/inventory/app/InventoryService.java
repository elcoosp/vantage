package com.vantage.inventory.app;

import com.vantage.core.exception.ResourceNotFoundException;
import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryResponse updateInventory(UUID productId, Long ifMatch, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        if (!inventory.getVersion().equals(ifMatch)) {
            throw new InventoryConflictException("Version mismatch. Expected: " + ifMatch + ", Actual: " + inventory.getVersion());
        }

        inventory.setQuantity(request.quantity());

        try {
            inventoryRepository.saveAndFlush(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InventoryConflictException("Concurrent modification detected for product: " + productId);
        }

        return new InventoryResponse(inventory.getProductId(), inventory.getQuantity(), inventory.getVersion());
    }
}
