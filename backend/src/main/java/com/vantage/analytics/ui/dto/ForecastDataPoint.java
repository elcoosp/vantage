package com.vantage.analytics.ui.dto;

import java.time.LocalDate;

public record ForecastDataPoint(LocalDate date, Integer predictedQuantity, Integer lowerBound, Integer upperBound) {
}
