package com.vantage.core.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("productCache", "forecastCache");
        // Note: Caffeine is recommended for production. Add spring-boot-starter-cache and caffeine dependencies,
        // then replace with CaffeineCacheManager to enable size/expiry limits.
        return cacheManager;
    }
}
