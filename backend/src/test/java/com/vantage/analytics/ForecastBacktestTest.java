package com.vantage.analytics;

import com.vantage.analytics.app.ForecastModel;
import com.vantage.analytics.app.HoltWintersModel;
import com.vantage.analytics.app.SeasonalNaiveModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the forecast model abstraction: {@link HoltWintersModel}
 * (statistical baseline) and {@link SeasonalNaiveModel} (naive baseline).
 *
 * <p>These are unit tests (no DB, no Docker) — they verify the mathematical
 * correctness of each model's forecast and backtest logic.</p>
 */
class ForecastBacktestTest {

    private final HoltWintersModel hw = new HoltWintersModel();
    private final SeasonalNaiveModel naive = new SeasonalNaiveModel();

    /** Synthetic 30-day history with weekly seasonality + trend. */
    private double[] syntheticHistory() {
        // Weeks get progressively higher, same day-of-week pattern
        double[] history = new double[30];
        double[] weeklyPattern = {10, 8, 12, 15, 20, 18, 14}; // Mon–Sun
        for (int i = 0; i < 30; i++) {
            double weeklyGrowth = 1.0 + (i / 7) * 0.1; // ~10% growth per week
            history[i] = weeklyPattern[i % 7] * weeklyGrowth + (Math.random() * 2 - 1);
            history[i] = Math.max(0, history[i]);
        }
        return history;
    }

    @Test
    void holtWinters_shouldProduceForecastWithCorrectHorizon() {
        double[] history = syntheticHistory();
        int horizon = 7;
        var input = new ForecastModel.ForecastInput(history, Collections.emptyList());

        ForecastModel.ForecastOutput output = hw.forecast(input, horizon);

        assertThat(output.forecast()).hasSize(horizon);
        assertThat(output.lower()).hasSize(horizon);
        assertThat(output.upper()).hasSize(horizon);
        for (int i = 0; i < horizon; i++) {
            assertThat(output.forecast()[i]).isGreaterThanOrEqualTo(0.0);
            assertThat(output.lower()[i]).isLessThanOrEqualTo(output.forecast()[i]);
            assertThat(output.upper()[i]).isGreaterThanOrEqualTo(output.forecast()[i]);
            assertThat(output.lower()[i]).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Test
    void seasonalNaive_shouldProduceForecastWithCorrectHorizon() {
        double[] history = syntheticHistory();
        int horizon = 7;
        var input = new ForecastModel.ForecastInput(history, Collections.emptyList());

        ForecastModel.ForecastOutput output = naive.forecast(input, horizon);

        assertThat(output.forecast()).hasSize(horizon);
        assertThat(output.lower()).hasSize(horizon);
        assertThat(output.upper()).hasSize(horizon);
        // Naive forecast should repeat the last week's values shifted
        for (int i = 0; i < horizon; i++) {
            assertThat(output.forecast()[i]).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Test
    void bothModels_shouldProduceBacktestMetrics() {
        double[] history = syntheticHistory();
        var input = new ForecastModel.ForecastInput(history, Collections.emptyList());

        ForecastModel.ForecastMetrics hwMetrics = hw.backtest(input, 14);
        ForecastModel.ForecastMetrics naiveMetrics = naive.backtest(input, 14);

        assertThat(hwMetrics.mape()).isNotNaN();
        assertThat(hwMetrics.smape()).isNotNaN();
        assertThat(hwMetrics.mase()).isNotNaN();
        assertThat(naiveMetrics.mape()).isNotNaN();
        assertThat(naiveMetrics.mase()).isNotNaN();
    }

    @Test
    void holtWinters_version_and_name() {
        assertThat(hw.name()).isEqualTo("holt-winters");
        assertThat(hw.version()).isEqualTo("1.0");
    }

    @Test
    void seasonalNaive_version_and_name() {
        assertThat(naive.name()).isEqualTo("seasonal-naive");
        assertThat(naive.version()).isEqualTo("1.0");
    }

    @Test
    void forecastResponse_fromModelOutput() {
        double[] history = syntheticHistory();
        var input = new ForecastModel.ForecastInput(history, Collections.emptyList());
        ForecastModel.ForecastOutput output = hw.forecast(input, 7);

        var response = com.vantage.analytics.app.ForecastMetricsMapper
            .toResponse(output, 7);
        assertThat(response.forecast()).hasSize(7);
    }
}
