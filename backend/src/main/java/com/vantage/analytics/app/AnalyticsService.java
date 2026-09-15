package com.vantage.analytics.app;

import com.vantage.analytics.ui.dto.ForecastDataPoint;
import com.vantage.analytics.ui.dto.ForecastResponse;
import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
public class AnalyticsService {

    private final EntityManager entityManager;
    private final List<ForecastModel> models;
    private final ForecastRunRepository forecastRunRepository;

    public AnalyticsService(
            EntityManager entityManager,
            List<ForecastModel> models,
            ForecastRunRepository forecastRunRepository) {
        this.entityManager = entityManager;
        this.models = models;
        this.forecastRunRepository = forecastRunRepository;
    }

    @Transactional(readOnly = true)
    public double[] getHistoricalData(UUID productId, int days) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }

        Instant start = Instant.now().minusSeconds(days * 86400L);

        String sql = """
                SELECT DATE(o.created_at) as day, SUM(o.quantity) as total
                FROM orders o
                WHERE o.product_id = :productId
                  AND o.tenant_id = :tenantId
                  AND o.created_at >= :startDate
                GROUP BY DATE(o.created_at)
                ORDER BY day
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);
        query.setParameter("tenantId", tenantId);
        query.setParameter("startDate", start);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        double[] history = new double[days];
        for (int i = 0; i < days; i++) {
            history[i] = 0.0;
        }

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = now.minusDays(days);
        for (Object[] row : results) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            long offset = java.time.temporal.ChronoUnit.DAYS.between(startDate, date);
            if (offset >= 0 && offset < days) {
                int idx = (int) offset;
                history[idx] = ((Number) row[1]).doubleValue();
            }
        }
        log.debug("Retrieved {} historical data points for product {}", days, productId);
        return history;
    }

    @Cacheable(value = "forecastCache", key = "#productId")
    public ForecastResponse getForecast(UUID productId) {
        log.debug("Computing forecast for product {}", productId);
        double[] history = getHistoricalData(productId, 30);

        ForecastModel.ForecastInput input = new ForecastModel.ForecastInput(
            history, Collections.emptyList());

        // Pick the best model by backtest MAPE
        ForecastModel bestModel = models.get(0);
        double bestMape = Double.MAX_VALUE;
        for (ForecastModel model : models) {
            try {
                ForecastModel.ForecastMetrics metrics = model.backtest(input, 14);
                log.debug("Model {} backtest MAPE: {}", model.name(), metrics.mape());
                if (metrics.mape() < bestMape && metrics.mape() > 0.0) {
                    bestMape = metrics.mape();
                    bestModel = model;
                }
            } catch (Exception e) {
                log.warn("Backtest failed for model {}: {}", model.name(), e.getMessage());
            }
        }

        ForecastModel.ForecastOutput output = bestModel.forecast(input, 7);
        ForecastModel.ForecastMetrics metrics = bestModel.backtest(input, 14);

        // Persist run for accuracy tracking / model versioning
        Map<String, Object> forecastPayload = Map.of(
            "forecast", output.forecast(),
            "lower", output.lower(),
            "upper", output.upper()
        );
        try {
            forecastRunRepository.saveRun(productId, bestModel.name(), bestModel.version(),
                7, metrics, forecastPayload);
        } catch (Exception e) {
            log.warn("Failed to persist forecast run: {}", e.getMessage());
        }

        List<ForecastDataPoint> points = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            int predicted = (int) Math.round(output.forecast()[i]);
            int lower = (int) Math.round(output.lower()[i]);
            int upper = (int) Math.round(output.upper()[i]);
            points.add(new ForecastDataPoint(date, predicted, lower, upper));
        }

        return new ForecastResponse(points);
    }
}
