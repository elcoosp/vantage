package com.vantage.analytics.app;

import java.util.List;
import java.util.Map;

/**
 * Interface for demand-forecast models.
 *
 * <p>Each implementation returns a forecast with point predictions and
 * confidence intervals. The {@link ForecastInput} carries the historical
 * series and optional exogenous features; the {@link ForecastOutput}
 * holds the predictions plus accuracy metadata.</p>
 *
 * <p>All models must be stateless w.r.t. tenant — the caller injects
 * the tenant-scoped {@link ForecastInput} and is responsible for
 * persistence via {@link ForecastRunRepository}.</p>
 */
public interface ForecastModel {

    /**
     * @return The model name, e.g. "holt-winters", "seasonal-naive".
     */
    String name();

    /**
     * @return A short version string for this model variant, e.g. "1.0".
     */
    String version();

    /**
     * Produce a forecast for the next {@code horizon} days.
     *
     * @param input  historical data and features
     * @param horizon number of days to forecast
     * @return forecast result with point predictions and intervals
     */
    ForecastOutput forecast(ForecastInput input, int horizon);

    /**
     * Compute accuracy metrics by backtesting on the input history.
     *
     * <p>Uses a rolling-origin evaluation: the model is fit on the first
     * {@code initialWindow} observations, forecasts the next day, and the
     * process is repeated (stepping forward) until the end of the history.
     *
     * @param input  full historical series
     * @param initialWindow minimum observations before forecasting begins
     * @return accuracy metrics (MAPE, sMAPE, MASE, pinball, coverage)
     */
    ForecastMetrics backtest(ForecastInput input, int initialWindow);

    /**
     * Input for a forecast model: historical daily sales quantity plus
     * optional exogenous features.
     */
    record ForecastInput(
        double[] history,
        List<ForecastFeature> features
    ) {}

    /**
     * Output of a forecast model: point predictions with confidence intervals.
     */
    record ForecastOutput(
        double[] forecast,
        double[] lower,
        double[] upper,
        Map<String, Object> metadata
    ) {}

    /**
     * Accuracy metrics for a forecast model, computed via backtesting.
     */
    record ForecastMetrics(
        double mape,
        double smape,
        double mase,
        double pinballLoss,
        double coverage,
        double mse
    ) {}
}
