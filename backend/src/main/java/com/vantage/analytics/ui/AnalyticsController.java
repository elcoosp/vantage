package com.vantage.analytics.ui;

import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    @GetMapping("/forecast/{productId}")
    public ForecastResponse getForecast(@PathVariable UUID productId) {
        // Stub: return empty list to make test fail
        return new ForecastResponse(List.of());
    }
}
