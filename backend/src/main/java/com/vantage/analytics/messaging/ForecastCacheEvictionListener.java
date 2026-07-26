package com.vantage.analytics.messaging;

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
        log.info("ForecastCacheEvictionListener created with cacheManager: {}", cacheManager);
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for product: {}, evicting forecastCache", event.getProductId());
        var cache = cacheManager.getCache("forecastCache");
        if (cache != null) {
            cache.evict(event.getProductId());
            log.info("Evicted forecastCache for product: {}", event.getProductId());
        } else {
            log.warn("forecastCache not found in cacheManager");
        }
    }
}
