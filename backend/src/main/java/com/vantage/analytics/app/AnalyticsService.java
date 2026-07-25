package com.vantage.analytics.app;

import lombok.extern.slf4j.Slf4j;
import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AnalyticsService {

    private final EntityManager entityManager;

    public AnalyticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
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
        // Fill with zeros
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
}
