package com.vantage.core.chat.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory per-tenant rate limiter (token budget + request budget).
 * In a clustered deployment this would be backed by Redis/Bucket4j.
 */
@Service
public class TokenRateLimiter {

    private final int maxTokensPerTenant;
    private final int maxRequestsPerTenant;

    private final Map<UUID, AtomicInteger> tokenBuckets = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> requestBuckets = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> windowStart = new ConcurrentHashMap<>();

    private static final long WINDOW_SECONDS = 60; // 1-minute window

    public TokenRateLimiter(
            @Value("${vantage.ai.max-tokens-per-tenant:40000}") int maxTokensPerTenant,
            @Value("${vantage.ai.max-requests-per-tenant:100}") int maxRequestsPerTenant) {
        this.maxTokensPerTenant = maxTokensPerTenant;
        this.maxRequestsPerTenant = maxRequestsPerTenant;
    }

    public boolean tryConsume(UUID tenantId, int tokensRequested) {
        Instant now = Instant.now();
        Instant window = windowStart.computeIfAbsent(tenantId, k -> now);
        if (now.getEpochSecond() - window.getEpochSecond() >= WINDOW_SECONDS) {
            windowStart.put(tenantId, now);
            tokenBuckets.put(tenantId, new AtomicInteger(maxTokensPerTenant));
            requestBuckets.put(tenantId, new AtomicInteger(maxRequestsPerTenant));
        }
        AtomicInteger tokenCount = tokenBuckets.computeIfAbsent(tenantId, k -> new AtomicInteger(maxTokensPerTenant));
        AtomicInteger requestCount = requestBuckets.computeIfAbsent(tenantId, k -> new AtomicInteger(maxRequestsPerTenant));
        if (requestCount.decrementAndGet() < 0) {
            return false;
        }
        return tokenCount.updateAndGet(v -> v >= tokensRequested ? v - tokensRequested : -1) >= 0;
    }
}
