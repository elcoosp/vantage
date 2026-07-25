package com.vantage.analytics.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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

        // Check if all values are zero
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
            if (count > 0 && sum != 0.0) {
                seasonalAvg[s] = sum / count;
            } else {
                seasonalAvg[s] = 1.0;
            }
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
        for (int i = seasonLength; i < 2 * seasonLength && i < n; i++) {
            trendSum += (deseasonalized[i] - deseasonalized[i - seasonLength]) / seasonLength;
        }
        trend[seasonLength - 1] = trendSum / Math.min(seasonLength, n - seasonLength);

        for (int i = 0; i < seasonLength; i++) {
            seasonal[i] = history[i] / level[seasonLength - 1];
        }

        for (int t = seasonLength; t < n; t++) {
            double prevLevel = level[t - 1];
            double prevTrend = trend[t - 1];
            double prevSeasonal = seasonal[t - seasonLength];
            // Guard against zero seasonal factor
            if (prevSeasonal == 0.0) prevSeasonal = 1.0;

            double newLevel = alpha * (history[t] / prevSeasonal) + (1 - alpha) * (prevLevel + prevTrend);
            double newTrend = beta * (newLevel - prevLevel) + (1 - beta) * prevTrend;
            // Guard against zero level when computing seasonal
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
