package com.vantage.analytics.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ForecastCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ForecastCacheEvictionListener.class);

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for product: {}", event.getProductId());
        // TODO: evict forecastCache
    }
}
