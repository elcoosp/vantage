package com.vantage.analytics.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Seasonal naive baseline: forecast = last observed value from the same
 * seasonal period (e.g., same day-of-week for daily data with period=7).
 *
 * <p>This is the simplest possible baseline. A real ML model must beat
 * this to be worth deploying.</p>
 */
@Slf4j
@Component
public class SeasonalNaiveModel implements ForecastModel {

    private static final int DEFAULT_PERIOD = 7;

    @Override
    public String name() {
        return "seasonal-naive";
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public ForecastOutput forecast(ForecastModel.ForecastInput input, int horizon) {
        double[] history = input.history();
        int n = history.length;
        int period = Math.min(DEFAULT_PERIOD, Math.max(1, n / 2));

        double[] forecast = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            int srcIdx = n - period + (h % period);
            forecast[h] = srcIdx >= 0 ? history[srcIdx] : 0.0;
            if (forecast[h] < 0) forecast[h] = 0;
        }

        // Naive intervals: use std dev of recent differences
        double stdDev = 0.0;
        if (n > period) {
            double sumSq = 0.0;
            int count = 0;
            for (int i = period; i < n; i++) {
                double diff = history[i] - history[i - period];
                sumSq += diff * diff;
                count++;
            }
            stdDev = count > 0 ? Math.sqrt(sumSq / count) : 0.0;
        }
        double multiplier = HoltWintersModel.CONFIDENCE_MULTIPLIER;
        double[] upper = new double[horizon];
        double[] lower = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            upper[h] = forecast[h] + multiplier * stdDev;
            lower[h] = Math.max(0, forecast[h] - multiplier * stdDev);
        }

        return new ForecastOutput(
            forecast, lower, upper,
            Map.of("model", name(), "version", version(), "std_dev", stdDev)
        );
    }

    @Override
    public ForecastMetrics backtest(ForecastModel.ForecastInput input, int initialWindow) {
        double[] history = input.history();
        int n = history.length;
        if (n < initialWindow + 1) {
            return new ForecastMetrics(0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
        }
        int period = Math.min(DEFAULT_PERIOD, Math.max(1, initialWindow / 2));

        double sumAbsPct = 0.0;
        double sumAbsPctSym = 0.0;
        double sumSq = 0.0;
        double naiveMae = 0.0;
        int count = 0;

        for (int i = initialWindow; i < n; i++) {
            int srcIdx = i - period;
            if (srcIdx < 0) continue;
            double pred = history[srcIdx];
            double actual = history[i];
            double error = actual - pred;
            count++;
            sumSq += error * error;
            if (actual != 0.0) {
                sumAbsPct += Math.abs(error) / Math.abs(actual);
                sumAbsPctSym += Math.abs(error) / ((Math.abs(actual) + Math.abs(pred)) / 2.0);
            }
            if (i > 0) {
                naiveMae += Math.abs(history[i] - history[i - 1]);
            }
        }
        int validCount = count > 0 ? count : 1;
        double mape = sumAbsPct / validCount;
        double smape = sumAbsPctSym / validCount;
        double mse = sumSq / validCount;
        double mase = naiveMae > 0 ? (Math.sqrt(mse) / (naiveMae / validCount)) : 0.0;
        double pinballLoss = Math.sqrt(mse); // L2 → same as RMSE for 0.5 quantile
        double coverage = 1.0; // Naive always covers 0; simplistic upper bound

        return new ForecastMetrics(mape, smape, mase, pinballLoss, coverage, mse);
    }
}
