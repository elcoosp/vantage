# TASK-009: Implement HMAC-Signed Webhooks

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Developer Experience, Webhook Rule)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 4.2: HMAC-Signed Webhooks)

## Objective
Implement a webhook delivery system. Vendors must be able to register a webhook URL. When an order reaches a terminal state (e.g., PAID, SHIPPED, CANCELLED), the system must construct a JSON payload, sign it using HMAC-SHA256, and deliver it to the vendor's URL. The delivery must use an exponential backoff retry strategy, culminating in a dead-letter queue after 5 failed attempts.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/integration/`, `backend/src/main/java/com/vantage/vendor/` (for registration endpoint), `backend/src/main/java/com/vantage/core/messaging/` (for RabbitMQ DLX config), and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `PaymentSucceededEvent`, `PaymentFailedEvent`)
- `docs/02-contracts/03-database-schema.md` (Reference: `vendors` table for webhook URL/secret)
- `backend/src/main/java/com/vantage/vendor/domain/Vendor.java`
- `backend/src/main/java/com/vantage/vendor/domain/VendorRepository.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`

## Acceptance Criteria

### 1. Webhook Registration
1. Add `webhookUrl` (String) and `webhookSecret` (String) fields to the `Vendor` entity.
2. Create `WebhookController` in `com.vantage.integration.ui` exposing `PUT /api/v1/webhooks`.
3. The endpoint must allow the authenticated vendor to update their `webhookUrl`. A new `webhookSecret` should be generated (UUID or secure random string) and saved, returning the secret to the user exactly once.

### 2. Webhook Dispatch Consumer
1. Create `WebhookDispatchConsumer` in `com.vantage.integration.messaging`.
2. Listen to `vantage.payment.events` and `vantage.order.events` for terminal order states (`PaymentSucceededEvent`, `PaymentFailedEvent`).
3. Check `ProcessedEventRepository` for idempotency.
4. Fetch the vendor's `webhookUrl` and `webhookSecret`.
5. Construct a unified `WebhookPayload` containing the `eventType`, `orderId`, `status`, and `occurredAt`.
6. Serialize the payload to JSON. Compute an HMAC-SHA256 signature of the JSON payload using the `webhookSecret`.
7. Publish a new internal message to a `vantage.webhook.delivery` queue. The message body must contain the `webhookUrl`, the JSON payload, and the signature.

### 3. Webhook Delivery & Retry
1. Create `WebhookDeliveryConsumer` in `com.vantage.integration.messaging`.
2. Listen to the `vantage.webhook.delivery` queue.
3. Use `RestTemplate` or `WebClient` to send an HTTP POST request to the `webhookUrl`.
4. Include headers: `Content-Type: application/json`, `X-Vantage-Signature: <hex_signature>`, and `X-Vantage-Event-Id: <event_id>`.
5. **Retry Logic:** If the HTTP call fails (timeout, 5xx, or 4xx), throw an `AmqpRejectAndDontRequeueException`.
6. **Dead Letter Queue:** Configure RabbitMQ with a Dead Letter Exchange (DLX) for the `vantage.webhook.delivery` queue. Set the max delivery attempts to 5 with exponential backoff (e.g., 1s, 5s, 15s, 60s) using TTL on a delay queue, or use Spring AMQP's retry interceptor.
7. If the HTTP call succeeds (2xx), ACK the message and save the `eventId` to `processed_events`.

### 4. Integration Testing (Testcontainers)
1. Create `WebhookDeliveryIT` in `backend/src/test/java/com/vantage/integration/`.
2. Setup: Register a vendor, update webhook URL to a local MockWebServer endpoint.
3. Publish a `PaymentSucceededEvent` to RabbitMQ.
4. Verify the MockWebServer receives the POST request.
5. Verify the `X-Vantage-Signature` header matches the expected HMAC-SHA256 of the payload.
6. **Failure Test:** Change the webhook URL to an invalid port. Publish an event. Verify the consumer attempts delivery 5 times and ultimately routes the message to the dead-letter queue (`vantage.webhook.dlq`).

## Target File Paths
- `backend/src/main/java/com/vantage/vendor/domain/Vendor.java` (Modify to add fields)
- `backend/src/main/java/com/vantage/integration/ui/WebhookController.java`
- `backend/src/main/java/com/vantage/integration/ui/dto/WebhookUpdateRequest.java`
- `backend/src/main/java/com/vantage/integration/app/WebhookPayload.java`
- `backend/src/main/java/com/vantage/integration/messaging/WebhookDispatchConsumer.java`
- `backend/src/main/java/com/vantage/integration/messaging/WebhookDeliveryConsumer.java`
- `backend/src/main/java/com/vantage/core/messaging/config/RabbitMQConfig.java` (Modify to add DLX)
- `backend/src/test/java/com/vantage/integration/WebhookDeliveryIT.java`
