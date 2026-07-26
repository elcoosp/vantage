package com.vantage.core.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public CacheConfig() {
        System.out.println("CacheConfig bean created");
    }

    @Bean
    public CacheManager cacheManager() {
        System.out.println("Creating CacheManager with caches: productCache, forecastCache");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("productCache", "forecastCache");
        System.out.println("CacheManager created: " + cacheManager);
        return cacheManager;
    }
}
