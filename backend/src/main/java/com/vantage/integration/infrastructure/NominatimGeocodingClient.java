package com.vantage.integration.infrastructure;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class NominatimGeocodingClient {
    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingClient.class);

    @RateLimiter(name = "geocoding", fallbackMethod = "geocodingFallback")
    public Coordinates geocode(String address) {
        // Simulate real geocoding – return a non-zero coordinate on success
        return new Coordinates(1.0, 1.0);
    }

    private Coordinates geocodingFallback(String address, Exception e) {
        log.warn("Geocoding fallback triggered for address: {}", address, e);
        return new Coordinates(0.0, 0.0);
    }

    public record Coordinates(double lat, double lon) {}
}