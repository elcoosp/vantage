package com.vantage.inventory.app;

import com.vantage.product.app.ProductCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProductCreatedEventListener {
    @EventListener
    public void handleProductCreatedEvent(ProductCreatedEvent event) {
        // stub
    }
}
