package com.vantage.analytics.ui;

import com.vantage.analytics.app.AnalyticsService;
import com.vantage.analytics.app.HoltWintersForecastCalculator;
import com.vantage.analytics.app.HoltWintersForecastCalculator.ForecastResult;
import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final HoltWintersForecastCalculator forecastCalculator;

    public AnalyticsController(AnalyticsService analyticsService, HoltWintersForecastCalculator forecastCalculator) {
        this.analyticsService = analyticsService;
        this.forecastCalculator = forecastCalculator;
    }

    @GetMapping("/forecast/{productId}")
    public ForecastResponse getForecast(@PathVariable UUID productId) {
        double[] history = analyticsService.getHistoricalData(productId, 30);
        ForecastResult result = forecastCalculator.forecast(history, 7);

        List<ForecastDataPoint> points = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            int predicted = (int) Math.round(result.forecast()[i]);
            int lower = (int) Math.round(result.lower()[i]);
            int upper = (int) Math.round(result.upper()[i]);
            points.add(new ForecastDataPoint(date, predicted, lower, upper));
        }

        return new ForecastResponse(points);
    }
}
