package com.vantage.core.developerlog.app;

import java.time.Instant;
import java.util.UUID;

/**
 * One captured API request as streamed to the developer portal.
 */
public record RequestLog(
        UUID id,
        String method,
        String path,
        int status,
        long durationMs,
        Instant at
) {
}
