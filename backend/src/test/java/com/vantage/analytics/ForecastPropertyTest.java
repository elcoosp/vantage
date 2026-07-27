package com.vantage.analytics;

import com.vantage.analytics.app.HoltWintersForecastCalculator;
import com.vantage.analytics.app.HoltWintersForecastCalculator.ForecastResult;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

public class ForecastPropertyTest {

    private final HoltWintersForecastCalculator calculator = new HoltWintersForecastCalculator();

    @Provide
    Arbitrary<double[]> history30Days() {
        return Arbitraries.doubles()
                .between(0.0, 1000.0)
                .array(double[].class).ofSize(30);
    }

    @Property(tries = 1000)
    void should_adhere_to_forecasting_invariants(@ForAll("history30Days") double[] history) {
        ForecastResult result = calculator.forecast(history, 7);

        for (int i = 0; i < result.lower().length; i++) {
            if (result.lower()[i] < 0.0) {
                throw new AssertionError("Lower bound is negative at index " + i + ": " + result.lower()[i]);
            }
        }

        for (int i = 0; i < result.forecast().length; i++) {
            double lower = result.lower()[i];
            double predicted = result.forecast()[i];
            double upper = result.upper()[i];

            if (!(lower <= predicted && predicted <= upper)) {
                throw new AssertionError("Invalid interval at index " + i + ": lower=" + lower + ", predicted=" + predicted + ", upper=" + upper);
            }
        }
    }
}
