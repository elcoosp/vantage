package com.vantage.analytics.ui;

import lombok.extern.slf4j.Slf4j;
import com.vantage.analytics.app.AnalyticsService;
import com.vantage.analytics.ui.dto.ForecastResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.vantage.api.api.ApiApi;

import java.util.UUID;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/analytics")
@Slf4j
public class AnalyticsController implements ApiApi {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/forecast/{productId}")
    public ForecastResponse getForecast(@PathVariable UUID productId) {
        log.debug("Forecast request for product {}", productId);
        return analyticsService.getForecast(productId);
    }

    @Override
    public ResponseEntity<com.vantage.api.model.ForecastResponse> apiV1AnalyticsForecastProductIdGet(UUID productId) {
        com.vantage.analytics.ui.dto.ForecastResponse internalResponse = analyticsService.getForecast(productId);
        com.vantage.api.model.ForecastResponse response = new com.vantage.api.model.ForecastResponse();
        response.setProductId(productId);
        java.util.List<com.vantage.api.model.ForecastResponseForecastInner> items = new java.util.ArrayList<>();
        for (com.vantage.analytics.ui.dto.ForecastDataPoint point : internalResponse.forecast()) {
            com.vantage.api.model.ForecastResponseForecastInner item = new com.vantage.api.model.ForecastResponseForecastInner()
                .date(point.date())
                .predictedQuantity(point.predictedQuantity())
                .lowerBound(point.lowerBound())
                .upperBound(point.upperBound());
            items.add(item);
        }
        response.setForecast(items);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}