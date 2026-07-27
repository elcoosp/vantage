package com.vantage.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.domain.ProcessedEvent;
import com.vantage.core.messaging.domain.ProcessedEventRepository;
import com.vantage.core.tenant.TenantContext;
import com.vantage.integration.app.WebhookPayload;
import com.vantage.core.events.PaymentFailedPayload;
import com.vantage.core.events.PaymentSucceededPayload;
import com.vantage.vendor.domain.Vendor;
import com.vantage.vendor.domain.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Component
public class WebhookDispatchConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchConsumer.class);

    private final ObjectMapper objectMapper;
    private final VendorRepository vendorRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public WebhookDispatchConsumer(ObjectMapper objectMapper,
                                   VendorRepository vendorRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   RabbitTemplate rabbitTemplate) {
        this.objectMapper = objectMapper;
        this.vendorRepository = vendorRepository;
        this.processedEventRepository = processedEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(bindings = {
        @QueueBinding(
            value = @Queue(name = "vantage.payment.events", durable = "true"),
            exchange = @Exchange(name = "vantage.events", type = "direct"),
            key = {"PaymentSucceededEvent", "PaymentFailedEvent"}
        )
    })
    @Transactional
    public void handlePaymentEvent(@Payload String payload,
                                   @Header("eventId") String eventIdHeader,
                                   @Header("amqp_receivedRoutingKey") String routingKey) {
        UUID eventId = UUID.fromString(eventIdHeader);
        try {
            if (processedEventRepository.existsById(eventId)) {
                log.info("Event {} already processed, skipping webhook dispatch", eventId);
                return;
            }

            // Determine event type and extract orderId/tenantId
            String eventType;
            UUID orderId;
            UUID tenantId;
            String status;

            if ("PaymentSucceededEvent".equals(routingKey)) {
                PaymentSucceededPayload event = objectMapper.readValue(payload, PaymentSucceededPayload.class);
                eventType = "PaymentSucceeded";
                orderId = event.orderId();
                tenantId = event.tenantId();
                status = "PAID";
            } else if ("PaymentFailedEvent".equals(routingKey)) {
                PaymentFailedPayload event = objectMapper.readValue(payload, PaymentFailedPayload.class);
                eventType = "PaymentFailed";
                orderId = event.orderId();
                tenantId = event.tenantId();
                status = "CANCELLED";
            } else {
                log.warn("Unhandled routing key: {}", routingKey);
                return;
            }

            TenantContext.setTenantId(tenantId);
            try {
                Vendor vendor = vendorRepository.findByTenantId(tenantId)
                        .orElseThrow(() -> new IllegalStateException("Vendor not found for tenant: " + tenantId));

                if (vendor.getWebhookUrl() == null || vendor.getWebhookSecret() == null) {
                    log.info("Vendor {} has no webhook configured, skipping dispatch", tenantId);
                    return;
                }

                // Mark as processed to prevent duplicate dispatch
                ProcessedEvent processed = new ProcessedEvent();
                processed.setEventId(eventId);
                processed.setTenantId(tenantId);
                processed.setProcessedAt(Instant.now());
                processedEventRepository.save(processed);

                WebhookPayload webhookPayload = new WebhookPayload(
                        eventType,
                        orderId,
                        status,
                        Instant.now()
                );

                String jsonPayload = objectMapper.writeValueAsString(webhookPayload);

                // Compute HMAC-SHA256 signature
                String secret = vendor.getWebhookSecret();
                String signature = hmacSha256(jsonPayload, secret);

                // Prepare internal message for delivery queue
                String deliveryMessage = objectMapper.writeValueAsString(
                        new DeliveryMessage(vendor.getWebhookUrl(), jsonPayload, signature, eventId.toString())
                );

                Message message = MessageBuilder
                        .withBody(deliveryMessage.getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("eventId", eventId.toString())
                        .build();

                rabbitTemplate.send("vantage.webhook.delivery.exchange", "webhook.delivery", message);
                log.info("Dispatched webhook delivery for event {}", eventId);

            } finally {
                TenantContext.clear();
            }

        } catch (Exception e) {
            log.error("Error dispatching webhook for event {}", eventId, e);
            throw new IllegalStateException("Failed to dispatch webhook", e);
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    // Internal DTO for delivery queue
    private static class DeliveryMessage {
        public String webhookUrl;
        public String payload;
        public String signature;
        public String eventId;

        public DeliveryMessage() {}

        public DeliveryMessage(String webhookUrl, String payload, String signature, String eventId) {
            this.webhookUrl = webhookUrl;
            this.payload = payload;
            this.signature = signature;
            this.eventId = eventId;
        }
    }
}