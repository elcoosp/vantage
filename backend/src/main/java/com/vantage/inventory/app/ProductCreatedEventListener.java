package com.vantage.inventory.app;

import com.vantage.inventory.domain.Inventory;
import com.vantage.inventory.domain.InventoryRepository;
import com.vantage.product.app.ProductCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductCreatedEventListener {

    private final InventoryRepository inventoryRepository;

    public ProductCreatedEventListener(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @EventListener
    @Transactional
    public void handleProductCreatedEvent(ProductCreatedEvent event) {
        Inventory inventory = new Inventory();
        inventory.setProductId(event.productId());
        inventory.setQuantity(0);
        inventoryRepository.save(inventory);
    }
}
