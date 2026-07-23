# TASK-008: Implement Idempotent Payment API

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Developer Experience, Idempotency Rule)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 4.1: Idempotent Payment API)

## Objective
Implement a public-facing `POST /api/v1/payments` endpoint that safely handles duplicate requests from external clients. It must require an `Idempotency-Key` header, cache the response for 24 hours, and return the cached response if a duplicate key is detected without reprocessing the payment.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/payment/` and its corresponding test directory.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `PaymentRequest`, `PaymentResponse`, `Idempotency-Key` header)
- `docs/02-contracts/03-database-schema.md` (Reference: `idempotency_keys` table)
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java`

## Acceptance Criteria

### 1. Idempotency Infrastructure
1. Create `IdempotencyKey` entity in `com.vantage.payment.domain` matching the `idempotency_keys` schema.
   - Fields: `id` (UUID, PK), `tenantId`, `requestHash` (String), `responseStatus` (Integer), `responseBody` (String/JSON), `expiresAt` (Instant).
2. Create `IdempotencyKeyRepository` extending `JpaRepository`.

### 2. API Controller & Service
1. Create `PaymentController` in `com.vantage.payment.ui` exposing `POST /api/v1/payments`.
2. The endpoint must require an `Idempotency-Key` header. If missing, return `400 Bad Request`.
3. Create `PaymentService` in `com.vantage.payment.app`:
   - Hash the incoming request body (SHA-256) to detect payload tampering.
   - Query `IdempotencyKeyRepository` by the provided key and `tenantId`.
   - If a record exists:
     - If the `requestHash` matches, return the cached `responseStatus` and `responseBody` immediately.
     - If the `requestHash` does NOT match, throw an `IdempotencyConflictException` (payload tampering detected).
   - If no record exists:
     - Process the mock payment (can be a simple synchronous success/fail based on amount).
     - Construct the `PaymentResponse`.
     - Save the `IdempotencyKey` record with the response payload and a 24-hour `expiresAt` TTL.
     - Return the response.

### 3. Exception Handling
1. Add a handler in `GlobalExceptionHandler` for `IdempotencyConflictException` returning `409 Conflict` with an appropriate error message.

### 4. Integration Testing (Testcontainers)
1. Create `IdempotentPaymentIT` in `backend/src/test/java/com/vantage/payment/`.
2. Test 1 (Success): Send a valid request with an `Idempotency-Key`. Assert `200 OK` and a `transactionId` is returned.
3. Test 2 (Duplicate): Send the exact same request with the same key. Assert `200 OK` with the exact same `transactionId`. Verify no second payment was processed (e.g., via a mock counter).
4. Test 3 (Tampering): Send a request with the same key but a different payload. Assert `409 Conflict`.

## Target File Paths
- `backend/src/main/java/com/vantage/payment/domain/IdempotencyKey.java`
- `backend/src/main/java/com/vantage/payment/domain/IdempotencyKeyRepository.java`
- `backend/src/main/java/com/vantage/payment/app/PaymentService.java`
- `backend/src/main/java/com/vantage/payment/app/IdempotencyConflictException.java`
- `backend/src/main/java/com/vantage/payment/ui/PaymentController.java`
- `backend/src/main/java/com/vantage/payment/ui/dto/PaymentRequest.java`
- `backend/src/main/java/com/vantage/payment/ui/dto/PaymentResponse.java`
- `backend/src/test/java/com/vantage/payment/IdempotentPaymentIT.java`
