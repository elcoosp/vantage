package com.vantage.core.websocket.app;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderGeoEstimatorTest {

    private final OrderGeoEstimator estimator = new OrderGeoEstimator();

    @Test
    void same_order_and_status_should_yield_identical_coordinates() {
        UUID orderId = UUID.randomUUID();
        double[] first = estimator.estimate(orderId, "SHIPPED");
        double[] second = estimator.estimate(orderId, "SHIPPED");
        assertThat(first).containsExactly(second[0], second[1]);
    }

    @Test
    void coordinates_should_stay_within_the_continental_us_bounding_box() {
        for (int i = 0; i < 500; i++) {
            double[] coords = estimator.estimate(UUID.randomUUID(), "SHIPPED");
            assertThat(coords[0]).isBetween(25.0, 49.0);  // lat
            assertThat(coords[1]).isBetween(-125.0, -66.0); // lon
        }
    }

    @Test
    void different_status_should_spread_the_pin() {
        UUID orderId = UUID.randomUUID();
        double[] shipped = estimator.estimate(orderId, "SHIPPED");
        double[] delivered = estimator.estimate(orderId, "DELIVERED");
        boolean moved = shipped[0] != delivered[0] || shipped[1] != delivered[1];
        assertThat(moved).isTrue();
    }
}
