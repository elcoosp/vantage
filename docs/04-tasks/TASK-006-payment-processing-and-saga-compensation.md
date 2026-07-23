# TASK-006: Implement Payment Processing, Resilience4j, and Saga Compensation

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Sections: Distributed Order Engine, Resilience and Fault Tolerance)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 3.2: Saga Compensation, Story 3.3: Resilient Payment Processing)

## Objective
Implement the `payment` module to process `InventoryReservedEvent`. Wrap the mock payment gateway call in Resilience4j `@Retry` and `@CircuitBreaker`. Implement the Saga Orchestrator in the `order` module to listen for `PaymentSucceededEvent` and `PaymentFailedEvent`, triggering the compensating transaction (`ReleaseInventoryEvent`) on failure.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/payment/`, `backend/src/main/java/com/vantage/order/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `PaymentSucceededEvent`, `PaymentFailedEvent`, `InventoryReleasedEvent`)
- `docs/00-product/02-features-and-business-rules.md`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/order/domain/OrderStatus.java`

## Acceptance Criteria

### 1. Payment Module & Mock Gateway
1. Create `MockPaymentGatewayClient` in `com.vantage.payment.infrastructure`.
   - Simulate a network call to a payment processor (e.g., `Thread.sleep(100)`).
   - Include a static flag or environment variable check to simulate a gateway failure (throwing `PaymentGatewayException`).

### 2. Resilience4j Configuration
1. Apply `@CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")` to the gateway client method.
2. Apply `@Retry(name = "payment")` to the gateway client method.
3. The fallback method must return a failed payment result, gracefully triggering the saga compensation without throwing an exception up the stack.

### 3. Payment Consumer
1. Create `PaymentInventoryConsumer` in `com.vantage.payment.messaging`.
2. Listen to the `vantage.inventory.events` queue for `InventoryReservedEvent`.
3. Check `ProcessedEventRepository` for idempotency.
4. Call the `MockPaymentGatewayClient`.
5. If successful, emit a `PaymentSucceededEvent` to the outbox.
6. If the circuit breaker is open or the gateway fails, emit a `PaymentFailedEvent` (reason: `GATEWAY_TIMEOUT` or `CIRCUIT_OPEN`) to the outbox.
7. Save the event ID to `processed_events` in the same transaction.

### 4. Saga Orchestrator (Order Module)
1. Create `PaymentSagaConsumer` in `com.vantage.order.messaging`.
2. Listen to the `vantage.payment.events` queue for `PaymentSucceededEvent` and `PaymentFailedEvent`.
3. **On Success:** Update the `Order` status to `PAID`.
4. **On Failure (Chaos Monkey):** 
   - Update the `Order` status to `CANCELLED`.
   - Construct an `InventoryReleasedPayload` (matching the AsyncAPI spec).
   - Emit an `InventoryReleasedEvent` to the outbox to trigger the compensating transaction.
5. Check `ProcessedEventRepository` for idempotency.

### 5. Integration Testing (Testcontainers - Chaos Monkey Scenario)
1. Create `PaymentSagaCompensationIT` in `backend/src/test/java/com/vantage/order/`.
2. Setup: Register vendor, create product, initialize inventory to 10.
3. Enable the "Simulate Payment Gateway Failure" flag on the `MockPaymentGatewayClient`.
4. Place an order for quantity 2.
5. Await at most 10 seconds for the Saga to execute.
6. Verify the `orders` table shows status `CANCELLED`.
7. Verify the `inventory` table shows quantity back at 10 (compensating transaction succeeded).
8. Verify the RabbitMQ queue received the `PaymentFailedEvent` and `InventoryReleasedEvent`.

## Target File Paths
- `backend/src/main/java/com/vantage/payment/infrastructure/MockPaymentGatewayClient.java`
- `backend/src/main/java/com/vantage/payment/infrastructure/PaymentGatewayException.java`
- `backend/src/main/java/com/vantage/payment/messaging/PaymentInventoryConsumer.java`
- `backend/src/main/java/com/vantage/payment/app/event/PaymentSucceededPayload.java`
- `backend/src/main/java/com/vantage/payment/app/event/PaymentFailedPayload.java`
- `backend/src/main/java/com/vantage/order/messaging/PaymentSagaConsumer.java`
- `backend/src/main/java/com/vantage/order/app/event/InventoryReleasedPayload.java`
- `backend/src/test/java/com/vantage/order/PaymentSagaCompensationIT.java`
