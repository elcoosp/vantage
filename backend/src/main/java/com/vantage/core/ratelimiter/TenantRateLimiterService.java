package com.vantage.core.ratelimiter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantRateLimiterService {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String tenantId) {
        return buckets.computeIfAbsent(tenantId, this::createBucket);
    }

    private Bucket createBucket(String tenantId) {
        // Interval-based refill: the full quota is restored only after the full period elapses,
        // so a burst of 100 requests is rejected on the 101st even if the burst spans many seconds.
        // (Greedy refill would trickle tokens back continuously and never reject a slow burst.)
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }
}
