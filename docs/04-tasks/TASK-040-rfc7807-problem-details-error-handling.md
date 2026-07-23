# TASK-040: Implement RFC 7807 Problem Details for REST API Error Handling

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand API contract maturity)
- Read: `docs/02-contracts/02-rest-api-spec.yaml` (Understand current error response structure)

## Objective
Refactor the global exception handling to comply with RFC 7807 (Problem Details for HTTP APIs). Instead of custom error JSON, all 4xx and 5xx responses must return a standardized `application/problem+json` payload. This demonstrates senior-level API design and interoperability, making the Vantage API easier for external developers (Persona 3) to consume and debug.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/exception/`, `backend/src/main/java/com/vantage/core/config/`, and their corresponding test directories.
- DO NOT modify domain logic, entities, or frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml`
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/vantage/inventory/app/InventoryConflictException.java`
- `backend/src/main/java/com/vantage/payment/app/IdempotencyConflictException.java`

## Acceptance Criteria

### 1. Problem Details Configuration
1. Add the `ProblemDetail` support (native to Spring Boot 3.x) to the `GlobalExceptionHandler`.
2. Ensure all responses are serialized with the `application/problem+json` media type.
3. Create a custom `ProblemDetail` factory method or use Spring's `ProblemDetail.forStatusAndDetail()` to standardize the output.

### 2. Exception Mapping
1. Map existing custom exceptions to RFC 7807 Problem Details:
   - `InventoryConflictException` (409): type=`https://vantage.io/errors/inventory-conflict`, title=`Inventory Conflict`, detail=`The inventory was modified by another transaction.`
   - `IdempotencyConflictException` (409): type=`https://vantage.io/errors/idempotency-conflict`, title=`Idempotency Conflict`, detail=`Payload tampering detected.`
   - `RateLimitExceededException` (429): type=`https://vantage.io/errors/rate-limit-exceeded`, title=`Rate Limit Exceeded`, detail=`Too many requests.`
2. Add properties to the Problem Details where appropriate (e.g., `retryAfter` for 429, `currentVersion` and `expectedVersion` for 409 inventory conflicts).
3. Ensure standard Spring exceptions (e.g., `MethodArgumentNotValidException` for 400 Bad Request) are also wrapped in the `application/problem+json` format.

### 3. Frontend Axios Interceptor Update (Verification Only)
1. Document that the frontend Axios interceptor should check for `error.response.headers['content-type'] === 'application/problem+json'` to parse the standardized error message for toast notifications.

### 4. Integration Testing (Testcontainers)
1. Create `ProblemDetailsIT` in `backend/src/test/java/com/vantage/core/exception/`.
2. Trigger an `InventoryConflictException` by sending a stale `If-Match` header.
3. Verify the response `Content-Type` is `application/problem+json`.
4. Verify the response body contains `type`, `title`, `status`, `detail`, and `instance` fields.
5. Verify the custom `currentVersion` property is present in the response.

## Target File Paths
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java` (Modify)
- `backend/src/main/java/com/vantage/core/exception/ProblemDetailFactory.java`
- `backend/src/test/java/com/vantage/core/exception/ProblemDetailsIT.java`
