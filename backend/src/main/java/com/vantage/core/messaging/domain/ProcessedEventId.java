package com.vantage.core.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite identity for {@link ProcessedEvent}.
 *
 * <p>Idempotency must be scoped per <em>consumer</em>, not just per event: multiple consumers
 * legitimately process the same {@code eventId} (for example both the inventory and the order-search
 * projector subscribe to {@code OrderCreatedEvent}). Keying the table solely on {@code event_id}
 * caused a primary-key collision when two consumers inserted the same event, which rolled back the
 * second consumer's transaction. Scoping the key with a {@code consumer} discriminator removes the
 * collision.</p>
 */
@Embeddable
public class ProcessedEventId implements Serializable {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer", nullable = false)
    private String consumer;

    public ProcessedEventId() {
    }

    public ProcessedEventId(UUID eventId, String consumer) {
        this.eventId = eventId;
        this.consumer = consumer;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProcessedEventId that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumer, that.consumer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumer);
    }
}
