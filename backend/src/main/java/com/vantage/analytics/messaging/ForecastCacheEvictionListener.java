package com.vantage.analytics.messaging;

import com.vantage.core.events.OrderCreatedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
public class ForecastCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ForecastCacheEvictionListener.class);
    private final CacheManager cacheManager;

    public ForecastCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        System.err.println("ForecastCacheEvictionListener created with cacheManager: " + cacheManager);
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        System.err.println("Listener invoked for product: " + event.getProductId());
        var cache = cacheManager.getCache("forecastCache");
        if (cache != null) {
            System.err.println("Evicting forecastCache for product: " + event.getProductId());
            cache.evict(event.getProductId());
            System.err.println("Evicted, eventReceived set to true");
        } else {
            System.err.println("forecastCache not found");
        }
    }


}