package com.vantage.inventory.ui;

import com.vantage.inventory.app.InventoryService;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Inventory management with optimistic locking (If-Match). Does not implement
 * the generated {@code ApiApi} interface — its default mappings would
 * duplicate these endpoints and break boot with ambiguous mappings.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> listInventory() {
        return inventoryService.listInventory();
    }

    /**
     * Items at or below the given on-hand threshold (dashboard low-stock
     * widget). Must be declared before the {productId} PUT so the literal
     * path segment wins.
     */
    @GetMapping("/low-stock")
    public List<InventoryResponse> lowStock(@RequestParam(name = "threshold", defaultValue = "10") int threshold) {
        return inventoryService.listInventory().stream()
                .filter(item -> item.quantity() <= threshold)
                .toList();
    }

    @PutMapping("/{productId}")
    public InventoryResponse update(@PathVariable UUID productId,
                                    @RequestHeader("If-Match") String ifMatch,
                                    @Valid @RequestBody InventoryUpdateRequest request) {
        return inventoryService.updateInventory(productId, Long.parseLong(ifMatch), request);
    }
}
