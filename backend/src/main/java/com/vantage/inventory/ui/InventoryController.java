package com.vantage.inventory.ui;

import com.vantage.inventory.app.InventoryService;
import com.vantage.inventory.ui.dto.InventoryResponse;
import com.vantage.inventory.ui.dto.InventoryUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.vantage.api.api.ApiApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController implements ApiApi {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PutMapping("/{productId}")
    public InventoryResponse update(@PathVariable UUID productId,
                                    @RequestHeader("If-Match") String ifMatch,
                                    @Valid @RequestBody InventoryUpdateRequest request) {
        return inventoryService.updateInventory(productId, Long.parseLong(ifMatch), request);
    }

    @Override
    public ResponseEntity<com.vantage.api.model.InventoryResponse> apiV1InventoryProductIdPut(UUID productId, Integer ifMatch, com.vantage.api.model.InventoryUpdateRequest inventoryUpdateRequest) {
        com.vantage.inventory.ui.dto.InventoryUpdateRequest internalRequest =
            new com.vantage.inventory.ui.dto.InventoryUpdateRequest(inventoryUpdateRequest.getQuantity());
        com.vantage.inventory.ui.dto.InventoryResponse internalResponse =
            inventoryService.updateInventory(productId, ifMatch.longValue(), internalRequest);
        com.vantage.api.model.InventoryResponse response = new com.vantage.api.model.InventoryResponse()
            .productId(internalResponse.productId())
            .quantity(internalResponse.quantity())
            .version(internalResponse.version().intValue());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}