package com.vantage.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
public class WebhookDeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryConsumer.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    public WebhookDeliveryConsumer(RestTemplate restTemplate,
                                   ObjectMapper objectMapper,
                                   RabbitTemplate rabbitTemplate,
                                   @Value("${vantage.webhook.maxAttempts:5}") int maxAttempts) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = "vantage.webhook.delivery.queue")
    public void handleDelivery(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            DeliveryMessage delivery = objectMapper.readValue(body, DeliveryMessage.class);

            // Check delivery count from headers
            Long deliveryCount = message.getMessageProperties().getHeader("x-delivery-count");
            int currentAttempt = (deliveryCount == null) ? 1 : deliveryCount.intValue() + 1;

            log.info("Webhook delivery attempt {} for event {}", currentAttempt, delivery.eventId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("X-Vantage-Signature", delivery.signature);
            headers.set("X-Vantage-Event-Id", delivery.eventId);

            HttpEntity<String> entity = new HttpEntity<>(delivery.payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    delivery.webhookUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook delivery succeeded for event {}", delivery.eventId);
                // We could store success in processed_events but already marked as processed in dispatch
                // ACK is automatic
            } else {
                log.warn("Webhook delivery returned {} for event {}", response.getStatusCode(), delivery.eventId);
                handleFailure(message, delivery, currentAttempt);
            }

        } catch (RestClientException e) {
            log.error("Webhook delivery failed: {}", e.getMessage());
            // We don't have the delivery object easily, but we can extract from message
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                DeliveryMessage delivery = objectMapper.readValue(body, DeliveryMessage.class);
                Long deliveryCount = message.getMessageProperties().getHeader("x-delivery-count");
                int currentAttempt = (deliveryCount == null) ? 1 : deliveryCount.intValue() + 1;
                handleFailure(message, delivery, currentAttempt);
            } catch (Exception ex) {
                log.error("Failed to parse delivery message", ex);
                throw new AmqpRejectAndDontRequeueException("Malformed message", ex);
            }
        }
    }

    private void handleFailure(Message message, DeliveryMessage delivery, int attempt) {
        if (attempt >= maxAttempts) {
            log.info("Webhook delivery failed after {} attempts, sending to DLQ for event {}", maxAttempts, delivery.eventId);
            // Reject and don't requeue, will go to DLX if configured
            throw new AmqpRejectAndDontRequeueException("Max attempts exceeded");
        } else {
            // Requeue with a delay or just let Spring retry? We'll use rabbit retry with backoff.
            // We can also use a delayed retry via TTL but simpler: let the listener container retry.
            // We'll throw an exception to trigger retry (if retry is configured)
            // But we want to avoid immediate retry; we'll use a custom delay by publishing to a delayed queue.
            // For simplicity, we'll just requeue by throwing a RuntimeException (not AmqpRejectAndDontRequeue)
            // and let the container retry immediately (not ideal).
            // Let's throw AmqpRejectAndDontRequeueException to move to DLQ after max attempts.
            // To implement exponential backoff, we would need a delayed exchange. For now we just retry up to max.
            // We'll increment a header and re-publish with a delay via TTL if we want.
            // Since we are already using the default retry interceptor, we can let it handle.
            // But we want to limit attempts. We'll use the retry interceptor configuration.
            // This is a placeholder: we'll throw RuntimeException to cause retry.
            throw new RuntimeException("Transient webhook failure, will retry");
        }
    }

    private static class DeliveryMessage {
        public String webhookUrl;
        public String payload;
        public String signature;
        public String eventId;
    }
}
