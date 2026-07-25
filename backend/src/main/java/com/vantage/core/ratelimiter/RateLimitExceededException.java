package com.vantage.core.ratelimiter;

import com.vantage.core.exception.VantageDomainException;

public class RateLimitExceededException extends VantageDomainException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
