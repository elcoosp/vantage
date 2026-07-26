package com.vantage.analytics.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ForecastCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ForecastCacheEvictionListener.class);
    private final CacheManager cacheManager;
    private static final AtomicBoolean eventReceived = new AtomicBoolean(false);

    public ForecastCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        log.info("ForecastCacheEvictionListener created");
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for product: {}", event.getProductId());
        System.out.println("Listener invoked for product: " + event.getProductId());
        var cache = cacheManager.getCache("forecastCache");
        if (cache != null) {
            System.out.println("Evicting forecastCache for product: " + event.getProductId());
            cache.evict(event.getProductId());
            eventReceived.set(true);
            System.out.println("Evicted");
        } else {
            System.out.println("forecastCache not found");
        }
    }

    public static boolean isEventReceived() {
        return eventReceived.get();
    }

    public static void resetEventReceived() {
        eventReceived.set(false);
    }
}
