package com.vantage.analytics.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Holt-Winters triple exponential smoothing model with additive trend
 * and multiplicative seasonality.
 *
 * <p>This is a legitimate statistical baseline — not AI/ML. It has no
 * training pipeline, no feature engineering, no model selection. Accuracy
 * is tracked via {@link ForecastRunRepository} for comparison with
 * future ML models.</p>
 */
@Slf4j
@Component
public class HoltWintersModel implements ForecastModel {

    private static final double DEFAULT_ALPHA = 0.3;
    private static final double DEFAULT_BETA = 0.1;
    private static final double DEFAULT_GAMMA = 0.3;
    private static final int SEASONALITY_PERIOD = 7;
    static final double CONFIDENCE_MULTIPLIER = 1.96;

    @Override
    public String name() {
        return "holt-winters";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public ForecastOutput forecast(ForecastModel.ForecastInput input, int horizon) {
        double[] history = input.history();
        ForecastResult result = forecast(history, horizon,
            DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_GAMMA, SEASONALITY_PERIOD);

        return new ForecastOutput(
            result.forecast(),
            result.lower(),
            result.upper(),
            Map.of("mse", result.mse(),
                   "model", name(),
                   "version", version())
        );
    }

    @Override
    public ForecastMetrics backtest(ForecastModel.ForecastInput input, int initialWindow) {
        double[] history = input.history();
        int n = history.length;
        if (n < initialWindow + 1) {
            return new ForecastMetrics(0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
        }

        double[] errors = new double[n - initialWindow];
        int count = 0;
        double sumAbsPct = 0.0;
        double sumAbsPctSym = 0.0;
        double sumSq = 0.0;

        // Naive baseline for MASE denominator
        double naiveMae = 0.0;
        for (int i = initialWindow; i < n; i++) {
            if (i > 0) naiveMae += Math.abs(history[i] - history[i - 1]);
        }
        naiveMae = naiveMae / Math.max(1, n - initialWindow);

        for (int i = initialWindow; i < n; i++) {
            double[] window = java.util.Arrays.copyOfRange(history, 0, i);
            // Use at least 2 full seasonal cycles or the full window
            int seasonLen = Math.min(SEASONALITY_PERIOD, window.length / 2);
            if (seasonLen < 1) seasonLen = 1;
            if (window.length < seasonLen * 2) {
                continue;
            }
            ForecastResult result = forecast(window, 1,
                DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_GAMMA, seasonLen);
            double pred = result.forecast()[0];
            double actual = history[i];
            double error = actual - pred;
            errors[count] = error;
            count++;
            sumSq += error * error;
            if (actual != 0.0) {
                sumAbsPct += Math.abs(error) / Math.abs(actual);
                sumAbsPctSym += Math.abs(error) / ((Math.abs(actual) + Math.abs(pred)) / 2.0);
            }
        }
        int validCount = count > 0 ? count : 1;
        double mape = sumAbsPct / validCount;
        double smape = sumAbsPctSym / validCount;
        double mse = sumSq / validCount;
        double mase = naiveMae > 0 ? (Math.sqrt(mse) / naiveMae) : 0.0;

        // Coverage: fraction of actuals within the prediction interval
        // (using the in-sample fitted values from the last forecast)
        double coverage = 0.0;
        if (n >= SEASONALITY_PERIOD * 2) {
            ForecastResult lastResult = forecast(history, 1,
                DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_GAMMA, SEASONALITY_PERIOD);
            double lower = lastResult.lower()[0];
            double upper = lastResult.upper()[0];
            double actual = history[n - 1];
            if (actual >= lower && actual <= upper) {
                coverage = 1.0;
            }
        }

        // Pinball loss (quantile regression at 0.5 = absolute error)
        double pinballLoss = 0.0;
        for (int j = 0; j < count; j++) {
            pinballLoss += Math.abs(errors[j]);
        }
        pinballLoss = pinballLoss / validCount;

        return new ForecastMetrics(mape, smape, mase, pinballLoss, coverage, mse);
    }

    // ---- Legacy API (kept for backward compat with AnalyticsService) ----

    public record ForecastResult(double[] forecast, double mse, double[] upper, double[] lower) {
    }

    public ForecastResult forecast(double[] history, int horizon) {
        return forecast(history, horizon, DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_GAMMA, SEASONALITY_PERIOD);
    }

    public ForecastResult forecast(double[] history, int horizon, double alpha, double beta, double gamma, int seasonLength) {
        int n = history.length;
        if (n < seasonLength * 2) {
            throw new IllegalArgumentException("History too short for seasonality");
        }

        boolean allZero = true;
        for (double v : history) {
            if (v != 0.0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            double[] zeros = new double[horizon];
            log.debug("All historical values are zero; returning zero forecast");
            return new ForecastResult(zeros, 0.0, zeros.clone(), zeros.clone());
        }

        double[] level = new double[n];
        double[] trend = new double[n];
        double[] seasonal = new double[n];

        double[] seasonalAvg = new double[seasonLength];
        for (int s = 0; s < seasonLength; s++) {
            double sum = 0.0;
            int count = 0;
            for (int i = s; i < n; i += seasonLength) {
                sum += history[i];
                count++;
            }
            seasonalAvg[s] = (count > 0 && sum != 0.0) ? sum / count : 1.0;
        }

        double[] deseasonalized = new double[n];
        for (int i = 0; i < n; i++) {
            deseasonalized[i] = history[i] / seasonalAvg[i % seasonLength];
        }

        double sumLevel = 0.0;
        for (int i = 0; i < seasonLength; i++) {
            sumLevel += deseasonalized[i];
        }
        level[seasonLength - 1] = sumLevel / seasonLength;

        double trendSum = 0.0;
        int trendCount = Math.min(seasonLength, Math.max(0, n - seasonLength));
        for (int i = seasonLength; i < 2 * seasonLength && i < n; i++) {
            trendSum += (deseasonalized[i] - deseasonalized[i - seasonLength]) / seasonLength;
        }
        trend[seasonLength - 1] = trendCount > 0 ? trendSum / trendCount : 0.0;

        for (int i = 0; i < seasonLength; i++) {
            seasonal[i] = history[i] / level[seasonLength - 1];
        }

        for (int t = seasonLength; t < n; t++) {
            double prevLevel = level[t - 1];
            double prevTrend = trend[t - 1];
            double prevSeasonal = seasonal[t - seasonLength];
            if (prevSeasonal == 0.0) prevSeasonal = 1.0;

            double newLevel = alpha * (history[t] / prevSeasonal) + (1 - alpha) * (prevLevel + prevTrend);
            double newTrend = beta * (newLevel - prevLevel) + (1 - beta) * prevTrend;
            double newLevelForSeasonal = (newLevel == 0.0) ? 1.0 : newLevel;
            double newSeasonal = gamma * (history[t] / newLevelForSeasonal) + (1 - gamma) * prevSeasonal;

            level[t] = newLevel;
            trend[t] = newTrend;
            seasonal[t] = newSeasonal;
        }

        double[] forecast = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            int t = n - 1;
            int seasonIndex = (t + 1 + h) % seasonLength;
            forecast[h] = (level[t] + (h + 1) * trend[t]) * seasonal[seasonIndex];
            if (forecast[h] < 0) forecast[h] = 0;
        }

        // In-sample fit for MSE and confidence intervals
        double sumSq = 0.0;
        int count = 0;
        for (int t = seasonLength; t < n; t++) {
            int seasonIndex = t % seasonLength;
            double fitted = (level[t - 1] + trend[t - 1]) * seasonal[seasonIndex];
            double error = history[t] - fitted;
            sumSq += error * error;
            count++;
        }
        double mse = count > 0 ? sumSq / count : 0.0;
        double stdDev = Math.sqrt(mse);
        double[] upper = new double[horizon];
        double[] lower = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            double pred = forecast[h];
            double interval = CONFIDENCE_MULTIPLIER * stdDev;
            upper[h] = pred + interval;
            lower[h] = Math.max(0, pred - interval);
        }

        return new ForecastResult(forecast, mse, upper, lower);
    }
}
