package com.vantage.analytics.ui.dto;

import java.util.List;

public record ForecastResponse(List<ForecastDataPoint> forecast) {
}
