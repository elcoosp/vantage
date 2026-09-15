package com.vantage.analytics.app;

/**
 * A single exogenous feature value for a day in the forecast input.
 *
 * <p>Examples: price, promotion_flag, is_holiday, stockout_mask,
 * lag_1, rolling_mean_7, day_of_week.</p>
 */
public record ForecastFeature(
    String name,
    double value
) {}
