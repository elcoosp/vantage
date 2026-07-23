# TASK-030: Implement Resilience4j Bulkhead and Rate Limiter

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Resilience and Fault Tolerance)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 3.3: Resilient Payment Processing)

## Objective
Enhance the Resilience4j configuration in the `payment` and `integration` modules. In addition to the existing `@Retry` and `@CircuitBreaker` from Task-006, apply `@Bulkhead` to limit concurrent calls to the external mock payment gateway (preventing thread pool exhaustion) and `@RateLimiter` to the Nominatim geocoding client to respect free-tier API limits (1 request/second).

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/payment/infrastructure/`, `backend/src/main/java/com/vantage/integration/infrastructure/`, `backend/src/main/resources/application.yml`, and their corresponding test directories.
- DO NOT modify domain logic, entities, or frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/00-product/02-features-and-business-rules.md`
- `backend/src/main/java/com/vantage/payment/infrastructure/MockPaymentGatewayClient.java`
- `backend/src/main/java/com/vantage/integration/infrastructure/NominatimGeocodingClient.java`
- `backend/src/main/resources/application.yml`

## Acceptance Criteria

### 1. Bulkhead Configuration for Payment Gateway
1. Apply `@Bulkhead(name = "payment", fallbackMethod = "paymentFallback")` to the `MockPaymentGatewayClient` method.
2. Configure the `payment` bulkhead in `application.yml`:
   - `maxConcurrentCalls: 5`
   - `maxWaitDuration: 0ms` (Fail fast if the bulkhead is full).
3. The fallback method should return a failed payment result, triggering the Saga compensation gracefully.

### 2. Rate Limiter Configuration for Nominatim Geocoding
1. Apply `@RateLimiter(name = "geocoding", fallbackMethod = "geocodingFallback")` to the `NominatimGeocodingClient` method.
2. Configure the `geocoding` rate limiter in `application.yml`:
   - `limitForPeriod: 1`
   - `limitRefreshPeriod: 1s`
   - `timeoutDuration: 2s` (Wait up to 2 seconds for a permission before failing).
3. The fallback method should return a default hardcoded coordinate (e.g., latitude 0.0, longitude 0.0) or throw a specific exception that the OpsMapConsumer can catch and skip broadcasting the pin.

### 3. Integration Testing (Testcontainers)
1. Modify `PaymentSagaCompensationIT` (or create a new `BulkheadIT`) in `backend/src/test/java/com/vantage/payment/`.
2. Simulate 10 concurrent payment requests.
3. Verify that only 5 proceed to the mock gateway, and the remaining 5 immediately trigger the fallback (and subsequent Saga compensation).
4. Modify `OpsMapWebSocketIT` (or create a new `RateLimiterIT`) in `backend/src/test/java/com/vantage/integration/`.
5. Simulate 5 rapid order shipments.
6. Verify that the geocoding client only processes 1 request per second, and the remaining calls either wait or hit the fallback coordinates.

## Target File Paths
- `backend/src/main/resources/application.yml` (Modify to add bulkhead/ratelimiter configs)
- `backend/src/main/java/com/vantage/payment/infrastructure/MockPaymentGatewayClient.java` (Modify)
- `backend/src/main/java/com/vantage/integration/infrastructure/NominatimGeocodingClient.java` (Modify)
- `backend/src/test/java/com/vantage/payment/BulkheadIT.java`
- `backend/src/test/java/com/vantage/integration/RateLimiterIT.java`
