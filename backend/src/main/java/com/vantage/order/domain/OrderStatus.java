// backend/src/main/java/com/vantage/order/domain/OrderStatus.java
package com.vantage.order.domain;

/**
 * Full order lifecycle.
 *
 * <p>Transitions:
 * CREATED → (saga: inventory reserved + payment captured) → PAID → SHIPPED → DELIVERED
 * CREATED/CONFIRMED/PAID → CANCELLED (compensation path).</p>
 *
 * <p>SHIPPED and DELIVERED were previously missing from this enum while seed
 * data and the search projection already used them, which made those rows
 * impossible to hydrate through JPA. They are now first-class states driven by
 * {@code PATCH /api/v1/orders/{id}/ship} and {@code PATCH /api/v1/orders/{id}/deliver}.</p>
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
