package com.vantage.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    public WebhookDeliveryConsumer(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "vantage.webhook.delivery.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleDelivery(Message message) {
        try {
            // Get retry count from header
            Long retryCount = message.getMessageProperties().getHeader("x-retry-count");
            if (retryCount == null) retryCount = 0L;
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            DeliveryMessage delivery = objectMapper.readValue(body, DeliveryMessage.class);

            log.info("Webhook delivery attempt {} for event {}", retryCount + 1, delivery.eventId);

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

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Webhook delivery returned {} for event {}", response.getStatusCode(), delivery.eventId);
                throw new RuntimeException("Webhook responded with non-2xx status");
            }
            log.info("Webhook delivery succeeded for event {}", delivery.eventId);

        } catch (RestClientException e) {
            Long retryCount = message.getMessageProperties().getHeader("x-retry-count");
            if (retryCount == null) retryCount = 0L;
            log.error("Webhook delivery failed on attempt {}: {}", retryCount + 1, e.getMessage());
            throw new RuntimeException("Webhook delivery failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during webhook delivery", e);
            throw new RuntimeException("Unexpected error", e);
        }
    }

    private static class DeliveryMessage {
        public String webhookUrl;
        public String payload;
        public String signature;
        public String eventId;
    }
}
