package com.vantage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.messaging.config.RabbitMQConfig;
import com.vantage.payment.app.event.PaymentSucceededPayload;
import com.vantage.vendor.ui.dto.AuthResponse;
import com.vantage.vendor.ui.dto.VendorRegistrationRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;
import java.util.Collection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.main.allow-bean-definition-overriding=true"
})
@Import({WebhookDeliveryIT.TestSecurityConfig.class, WebhookDeliveryIT.TestRabbitMQConfig.class})
@Testcontainers
public class WebhookDeliveryIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class TestRabbitMQConfig {
        @Bean
        @Primary
        public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
            SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setDefaultRequeueRejected(false);

            RetryTemplate retryTemplate = new RetryTemplate();
            SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
            retryPolicy.setMaxAttempts(5);
            retryTemplate.setRetryPolicy(retryPolicy);

            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(1000);
            backOffPolicy.setMultiplier(2.0);
            backOffPolicy.setMaxInterval(5000);
            retryTemplate.setBackOffPolicy(backOffPolicy);

            factory.setRetryTemplate(retryTemplate);
            return factory;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672)
            .waitingFor(Wait.forListeningPorts(5672, 15672)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.primary.username", postgres::getUsername);
        registry.add("spring.datasource.primary.password", postgres::getPassword);
        registry.add("spring.datasource.replica.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.replica.username", postgres::getUsername);
        registry.add("spring.datasource.replica.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "CORRELATED");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        System.out.println("RabbitMQ host: " + rabbitmq.getHost());
        System.out.println("RabbitMQ port: " + rabbitmq.getAmqpPort());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Test
    void should_deliver_webhook_with_correct_signature_when_payment_succeeded() throws Exception {
        // Register a vendor and set up webhook
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "webhook-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        // Update webhook URL to a local mock server
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        String webhookUrl = mockWebServer.url("/webhook").toString();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        com.vantage.integration.ui.dto.WebhookUpdateRequest updateRequest =
            new com.vantage.integration.ui.dto.WebhookUpdateRequest(webhookUrl);
        HttpEntity<com.vantage.integration.ui.dto.WebhookUpdateRequest> updateEntity =
            new HttpEntity<>(updateRequest, headers);
        ResponseEntity<com.vantage.integration.ui.dto.WebhookUpdateResponse> updateRes =
            restTemplate.exchange("/api/v1/webhooks", HttpMethod.PUT, updateEntity,
                com.vantage.integration.ui.dto.WebhookUpdateResponse.class);
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String webhookSecret = updateRes.getBody().secret();

        // Publish a PaymentSucceededEvent
        UUID orderId = UUID.randomUUID();
        PaymentSucceededPayload payload = new PaymentSucceededPayload(orderId, tenantId);
        String jsonPayload = objectMapper.writeValueAsString(payload);
        UUID eventId = UUID.randomUUID();
        Message message = MessageBuilder
            .withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("eventId", eventId.toString())
            .build();
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, "PaymentSucceededEvent", message);

        System.out.println("Published PaymentSucceededEvent with eventId: " + eventId);

        // Wait for webhook to be received
        System.out.println("Waiting for webhook delivery...");
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                RecordedRequest recordedRequest = mockWebServer.takeRequest();
                assertThat(recordedRequest).isNotNull();
                assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                assertThat(recordedRequest.getHeader("Content-Type")).startsWith("application/json");
                assertThat(recordedRequest.getHeader("X-Vantage-Event-Id")).isEqualTo(eventId.toString());

                String signatureHeader = recordedRequest.getHeader("X-Vantage-Signature");
                assertThat(signatureHeader).isNotBlank();

                String body = recordedRequest.getBody().readUtf8();
                String expectedSignature = hmacSha256(body, webhookSecret);
                assertThat(signatureHeader).isEqualTo(expectedSignature);

                com.vantage.integration.app.WebhookPayload webhookPayload =
                    objectMapper.readValue(body, com.vantage.integration.app.WebhookPayload.class);
                assertThat(webhookPayload.eventType()).isEqualTo("PaymentSucceeded");
                assertThat(webhookPayload.orderId()).isEqualTo(orderId);
                assertThat(webhookPayload.status()).isEqualTo("PAID");
            });

        mockWebServer.shutdown();
    }

    @Test
    void should_send_to_dlq_after_max_retries_when_webhook_unreachable() throws Exception {
        // Register a vendor and set up webhook with invalid URL
        VendorRegistrationRequest vendorReq = new VendorRegistrationRequest(
            "webhook-fail-" + UUID.randomUUID() + "@vantage.com",
            "securePassword123",
            "Vantage Inc.");
        HttpHeaders vendorHeaders = new HttpHeaders();
        vendorHeaders.setContentType(MediaType.APPLICATION_JSON);
        vendorHeaders.set("X-Tenant-ID", UUID.randomUUID().toString());
        HttpEntity<VendorRegistrationRequest> vendorEntity = new HttpEntity<>(vendorReq, vendorHeaders);
        ResponseEntity<AuthResponse> vendorRes = restTemplate.postForEntity(
            "/api/v1/vendors/register", vendorEntity, AuthResponse.class);
        assertThat(vendorRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = vendorRes.getBody().token();
        UUID tenantId = vendorRes.getBody().tenantId();

        // Update webhook URL to an invalid port
        String invalidWebhookUrl = "http://localhost:9999/webhook";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId.toString());

        com.vantage.integration.ui.dto.WebhookUpdateRequest updateRequest =
            new com.vantage.integration.ui.dto.WebhookUpdateRequest(invalidWebhookUrl);
        HttpEntity<com.vantage.integration.ui.dto.WebhookUpdateRequest> updateEntity =
            new HttpEntity<>(updateRequest, headers);
        ResponseEntity<com.vantage.integration.ui.dto.WebhookUpdateResponse> updateRes =
            restTemplate.exchange("/api/v1/webhooks", HttpMethod.PUT, updateEntity,
                com.vantage.integration.ui.dto.WebhookUpdateResponse.class);
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Publish PaymentSucceededEvent
        UUID orderId = UUID.randomUUID();
        PaymentSucceededPayload payload = new PaymentSucceededPayload(orderId, tenantId);
        String jsonPayload = objectMapper.writeValueAsString(payload);
        UUID eventId = UUID.randomUUID();
        Message message = MessageBuilder
            .withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setHeader("eventId", eventId.toString())
            .build();
        rabbitTemplate.send(RabbitMQConfig.EXCHANGE, "PaymentSucceededEvent", message);
        System.out.println("Published event for DLQ test with eventId: " + eventId);

        // Wait and verify the DLQ queue has one message
        Awaitility.await()
            .atMost(Duration.ofSeconds(120))
            .pollInterval(Duration.ofSeconds(2))
            .until(() -> {
                Properties props = rabbitAdmin.getQueueProperties("vantage.webhook.dlq");
                if (props == null) {
                    System.out.println("DLQ queue properties are null - queue may not exist");
                    return false;
                }
                Object msgCount = props.get("QUEUE_MESSAGE_COUNT");
                System.out.println("DLQ message count: " + msgCount);
                if (msgCount instanceof Integer) {
                    return (Integer) msgCount > 0;
                }
                return false;
            });

        // Verify by receiving one message
        Message dlqMessage = rabbitTemplate.receive("vantage.webhook.dlq");
        assertThat(dlqMessage).isNotNull();
        String dlqBody = new String(dlqMessage.getBody());
        assertThat(dlqBody).contains(eventId.toString());
        assertThat(dlqBody).contains("http://localhost:9999/webhook");
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
}
