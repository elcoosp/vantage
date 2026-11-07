package com.vantage.analytics.ui;

import lombok.extern.slf4j.Slf4j;
import com.vantage.analytics.app.AnalyticsService;
import com.vantage.analytics.ui.dto.ForecastResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Analytics (demand forecasting). Does not implement the generated
 * {@code ApiApi} interface — its default mappings would duplicate these
 * endpoints and break boot with ambiguous mappings.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/forecast/{productId}")
    public ForecastResponse getForecast(@PathVariable UUID productId) {
        log.debug("Forecast request for product {}", productId);
        return analyticsService.getForecast(productId);
    }

}