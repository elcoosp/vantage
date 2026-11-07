package com.vantage.core.websocket.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.events.OrderCreatedPayload;
import com.vantage.core.events.OrderStatusChangedPayload;
import com.vantage.core.events.PaymentFailedPayload;
import com.vantage.core.events.PaymentSucceededPayload;
import com.vantage.core.websocket.dto.OpsMapPinPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Bridges order lifecycle events from the {@code vantage.events} exchange to
 * the browser: every event becomes a pin on the tenant's live ops map.
 *
 * <p>This is the missing backend half of the Live Ops Map feature — the
 * frontend has been subscribing to {@code /topic/ops-map} from day one, but no
 * publisher ever existed.</p>
 */
@Component
public class OpsMapPublisher {

    private static final Logger log = LoggerFactory.getLogger(OpsMapPublisher.class);
    public static final String TOPIC_PREFIX = "/topic/ops-map.";

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderGeoEstimator geoEstimator;
    private final ObjectMapper objectMapper;

    public OpsMapPublisher(SimpMessagingTemplate messagingTemplate, OrderGeoEstimator geoEstimator, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.geoEstimator = geoEstimator;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(name = "vantage.ops.map.events", durable = "true"),
                    exchange = @Exchange(name = "vantage.events", type = "direct"),
                    key = {"OrderCreatedEvent", "OrderShippedEvent", "OrderDeliveredEvent",
                            "OrderCancelledEvent", "PaymentSucceededEvent", "PaymentFailedEvent"}
            )
    })
    public void handleOrderLifecycleEvent(@Payload String payload,
                                          @Header("amqp_receivedRoutingKey") String routingKey) {
        try {
            switch (routingKey) {
                case "OrderCreatedEvent" -> {
                    OrderCreatedPayload event = objectMapper.readValue(payload, OrderCreatedPayload.class);
                    publish(event.orderId(), event.tenantId(), event.productName(), "CREATED");
                }
                case "OrderShippedEvent", "OrderDeliveredEvent", "OrderCancelledEvent" -> {
                    OrderStatusChangedPayload event = objectMapper.readValue(payload, OrderStatusChangedPayload.class);
                    publish(event.orderId(), event.tenantId(), event.productName(), event.newStatus());
                }
                case "PaymentSucceededEvent" -> {
                    PaymentSucceededPayload event = objectMapper.readValue(payload, PaymentSucceededPayload.class);
                    publish(event.orderId(), event.tenantId(), null, "PAID");
                }
                case "PaymentFailedEvent" -> {
                    PaymentFailedPayload event = objectMapper.readValue(payload, PaymentFailedPayload.class);
                    publish(event.orderId(), event.tenantId(), null, "CANCELLED");
                }
                default -> log.debug("Ignoring routing key {} on ops map queue", routingKey);
            }
        } catch (Exception e) {
            // Never rethrow: a malformed pin must not poison the queue (the
            // listener container does not requeue by default, but staying
            // defensive keeps one bad payload from disturbing the broker).
            log.error("Failed to publish ops-map pin for routing key {}", routingKey, e);
        }
    }

    private void publish(UUID orderId, UUID tenantId, String productName, String status) {
        if (tenantId == null || orderId == null) {
            return;
        }
        double[] coords = geoEstimator.estimate(orderId, status);
        OpsMapPinPayload pin = new OpsMapPinPayload(
                orderId.toString(),
                coords[0],
                coords[1],
                productName == null ? "" : productName,
                status,
                Instant.now().toString());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + tenantId, pin);
        log.debug("Published ops-map pin {} for tenant {}", status, tenantId);
    }
}
