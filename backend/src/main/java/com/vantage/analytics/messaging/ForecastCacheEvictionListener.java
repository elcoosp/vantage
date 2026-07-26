package com.vantage.analytics.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ForecastCacheEvictionListener implements ApplicationListener<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ForecastCacheEvictionListener.class);
    private final CacheManager cacheManager;
    private static final AtomicBoolean eventReceived = new AtomicBoolean(false);

    public ForecastCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        log.info("ForecastCacheEvictionListener initialized");
    }

    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for product: {}, evicting forecastCache", event.getProductId());
        cacheManager.getCache("forecastCache").evict(event.getProductId());
        eventReceived.set(true);
    }

    public static boolean isEventReceived() {
        return eventReceived.get();
    }

    public static void resetEventReceived() {
        eventReceived.set(false);
    }
}
