package com.vantage.analytics.app;

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
        for (Object[] row : results) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            long diffDays = java.time.temporal.ChronoUnit.DAYS.between(date, now);
            if (diffDays >= 0 && diffDays < days) {
                int index = (int) diffDays; // oldest at index 0? Actually we want last 30 days, most recent at end
                // We want index 0 = oldest, index 29 = yesterday
                // diffDays = days between date and now: if date is yesterday, diffDays=1, index should be days-1 - (diffDays-1)? Let's simplify: we want array where index 0 = day (now-30), index 29 = yesterday.
                // So we set index = days - 1 - (int) diffDays? Wait: if date is now - 1 day, diffDays=1, we want index 29? Actually if days=30, we want index 0 for day (now-30), index 29 for (now-1). So index = days - 1 - diffDays? Let's test: if date is now-30, diffDays=30, days-1 - 30 = -1, not good. So we need to compute days between startDate and date? Better: compute offset from startDate.
                // Use startDate = now - days days. Then diff from start = days between start and date.
                LocalDate startDate = now.minusDays(days);
                long offset = java.time.temporal.ChronoUnit.DAYS.between(startDate, date);
                if (offset >= 0 && offset < days) {
                    int idx = (int) offset; // 0 for oldest, days-1 for latest
                    history[idx] = ((Number) row[1]).doubleValue();
                }
            }
        }

        return history;
    }
}
