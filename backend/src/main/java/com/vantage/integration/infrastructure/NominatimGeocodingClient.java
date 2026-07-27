package com.vantage.integration.infrastructure;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.springframework.stereotype.Component;

@Component
public class NominatimGeocodingClient {

    @RateLimiter(name = "geocoding", fallbackMethod = "geocodingFallback")
    public Coordinates geocode(String address) {
        // Dummy implementation – will be replaced with real logic later
        return new Coordinates(0.0, 0.0);
    }

    private Coordinates geocodingFallback(String address, Exception e) {
        return new Coordinates(0.0, 0.0);
    }

    public record Coordinates(double lat, double lon) {}
}