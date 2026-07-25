package com.vantage.analytics.app;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class HoltWintersForecastCalculator {

    private static final double DEFAULT_ALPHA = 0.3;
    private static final double DEFAULT_BETA = 0.1;
    private static final double DEFAULT_GAMMA = 0.3;
    private static final int SEASONALITY_PERIOD = 7;

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

        // Initial level: average of first season
        double[] level = new double[n];
        double[] trend = new double[n];
        double[] seasonal = new double[n];

        // Initialize seasonal indices with average for each position in season
        double[] seasonalAvg = new double[seasonLength];
        int fullSeasons = n / seasonLength;
        for (int s = 0; s < seasonLength; s++) {
            double sum = 0.0;
            int count = 0;
            for (int i = s; i < n; i += seasonLength) {
                sum += history[i];
                count++;
            }
            seasonalAvg[s] = sum / count;
        }
        // De-seasonalize and get initial level & trend
        double[] deseasonalized = new double[n];
        for (int i = 0; i < n; i++) {
            deseasonalized[i] = history[i] / seasonalAvg[i % seasonLength];
        }
        // Initial level: average of first season's deseasonalized
        double sumLevel = 0.0;
        for (int i = 0; i < seasonLength; i++) {
            sumLevel += deseasonalized[i];
        }
        level[seasonLength - 1] = sumLevel / seasonLength;
        // Initial trend: average of differences between seasons
        double trendSum = 0.0;
        for (int i = seasonLength; i < 2 * seasonLength && i < n; i++) {
            trendSum += (deseasonalized[i] - deseasonalized[i - seasonLength]) / seasonLength;
        }
        trend[seasonLength - 1] = trendSum / Math.min(seasonLength, n - seasonLength);

        // Initialize seasonal for first season
        for (int i = 0; i < seasonLength; i++) {
            seasonal[i] = history[i] / level[seasonLength - 1];
        }

        // Smoothing
        for (int t = seasonLength; t < n; t++) {
            double prevLevel = level[t - 1];
            double prevTrend = trend[t - 1];
            double prevSeasonal = seasonal[t - seasonLength];

            double newLevel = alpha * (history[t] / prevSeasonal) + (1 - alpha) * (prevLevel + prevTrend);
            double newTrend = beta * (newLevel - prevLevel) + (1 - beta) * prevTrend;
            double newSeasonal = gamma * (history[t] / newLevel) + (1 - gamma) * prevSeasonal;

            level[t] = newLevel;
            trend[t] = newTrend;
            seasonal[t] = newSeasonal;
        }

        // Forecast
        double[] forecast = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            int t = n - 1;
            int seasonIndex = (t + 1 + h) % seasonLength;
            forecast[h] = (level[t] + (h + 1) * trend[t]) * seasonal[seasonIndex];
            if (forecast[h] < 0) forecast[h] = 0;
        }

        // Compute MSE on in-sample fit (from seasonLength to n-1)
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

        // Confidence intervals: 95% (approx 1.96 * sqrt(MSE))
        double stdDev = Math.sqrt(mse);
        double multiplier = 1.96;
        double[] upper = new double[horizon];
        double[] lower = new double[horizon];
        for (int h = 0; h < horizon; h++) {
            double pred = forecast[h];
            double interval = multiplier * stdDev;
            upper[h] = pred + interval;
            lower[h] = Math.max(0, pred - interval);
        }

        return new ForecastResult(forecast, mse, upper, lower);
    }
}
