package com.vantage.core.ratelimiter;

import com.vantage.core.tenant.TenantContext;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final TenantRateLimiterService rateLimiterService;

    public RateLimitInterceptor(TenantRateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true;
        }

        Bucket bucket = rateLimiterService.resolveBucket(tenantId.toString());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            // Use a fixed retry-after of 60 seconds (the refill period).
            // A dynamic calculation using probe.getNanosToWaitForToken() is not available in the used version.
            long retryAfterSeconds = 60;
            throw new RateLimitExceededException("Rate limit exceeded for tenant: " + tenantId, retryAfterSeconds);
        }
        return true;
    }
}
