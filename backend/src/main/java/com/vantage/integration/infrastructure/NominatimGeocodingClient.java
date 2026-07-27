package com.vantage.integration.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class NominatimGeocodingClient {

    public Coordinates geocode(String address) {
        // Dummy implementation – will be replaced with real logic and rate limiter in green phase
        return new Coordinates(0.0, 0.0);
    }

    public record Coordinates(double lat, double lon) {}
}
