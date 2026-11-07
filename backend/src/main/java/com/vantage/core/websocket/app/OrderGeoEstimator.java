package com.vantage.core.websocket.app;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Deterministic, privacy-preserving fulfillment geo estimates.
 *
 * <p>Orders do not carry customer addresses (by design), so instead of calling
 * an external geocoder with made-up input the ops map derives a stable point
 * from the order id: SHA-256(orderId + status) mapped into the continental
 * US bounding box, pulled slightly towards one of a few fulfillment hubs so
 * the map clusters believably. Same order + same status always yields the
 * same pin.</p>
 */
@Component
public class OrderGeoEstimator {

    private static final double MIN_LAT = 25.0;
    private static final double MAX_LAT = 49.0;
    private static final double MIN_LON = -125.0;
    private static final double MAX_LON = -66.0;

    /** Fulfillment hub coordinates the estimates gravitate towards. */
    private static final double[][] HUBS = {
            {40.7128, -74.0060},   // New York
            {34.0522, -118.2437},  // Los Angeles
            {41.8781, -87.6298},   // Chicago
            {29.7604, -95.3698},   // Houston
            {39.7392, -104.9903},  // Denver
    };

    /**
     * @param seed stable id (order id) deciding the pin location
     * @param variation second input (status) so lifecycle stages spread slightly
     * @return {lat, lon} inside the continental US bounding box
     */
    public double[] estimate(UUID seed, String variation) {
        byte[] digest = sha256(seed + "|" + variation);
        long a = readLong(digest, 0);
        long b = readLong(digest, 8);
        double unitA = (a & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL;
        double unitB = (b & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL;

        double[] hub = HUBS[(int) (unitB * HUBS.length) % HUBS.length];

        // 80% of the pull towards the hub, 20% uniform across the map.
        double lat = hub[0] * 0.8 + (MIN_LAT + unitA * (MAX_LAT - MIN_LAT)) * 0.2;
        double lon = hub[1] * 0.8 + (MIN_LON + unitB * (MAX_LON - MIN_LON)) * 0.2;

        // Clamp to the bounding box (hub blends can exceed it on the coasts).
        lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));
        lon = Math.max(MIN_LON, Math.min(MAX_LON, lon));
        return new double[]{lat, lon};
    }

    private static byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static long readLong(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getLong(offset);
    }
}
