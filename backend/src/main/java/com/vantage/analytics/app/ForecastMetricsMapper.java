package com.vantage.analytics.app;

import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts {@link ForecastModel.ForecastOutput} into the API DTO
 * {@link ForecastResponse} and persists accuracy via
 * {@link ForecastRunRepository}.
 */
public final class ForecastMetricsMapper {

    private ForecastMetricsMapper() {}

    public static ForecastResponse toResponse(ForecastModel.ForecastOutput output, int horizon) {
        double[] forecast = output.forecast();
        double[] lower = output.lower();
        double[] upper = output.upper();
        List<ForecastDataPoint> points = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        for (int i = 0; i < horizon && i < forecast.length; i++) {
            LocalDate date = start.plusDays(i);
            int predicted = (int) Math.round(forecast[i]);
            int lb = (int) Math.round(lower[i]);
            int ub = (int) Math.round(upper[i]);
            points.add(new ForecastDataPoint(date, predicted, lb, ub));
        }
        return new ForecastResponse(points);
    }
}
