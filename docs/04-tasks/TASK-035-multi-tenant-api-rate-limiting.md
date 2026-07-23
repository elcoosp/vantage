# TASK-035: Implement Multi-Tenant API Rate Limiting (Bucket4j)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Understand SaaS platform protection)
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Resilience and Fault Tolerance)

## Objective
Protect the Vantage platform from "noisy neighbor" scenarios by implementing per-tenant API rate limiting. Use Bucket4j to enforce a limit of 100 requests per minute per tenant. When the limit is exceeded, the API must respond with `429 Too Many Requests` and a `Retry-After` header, demonstrating senior-level API management and platform protection.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/ratelimiter/`, `backend/src/main/java/com/vantage/core/config/` (for WebMvc config), and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or domain-specific controllers.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/java/com/vantage/core/tenant/TenantContext.java`
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java`

## Acceptance Criteria

### 1. Bucket4j Configuration
1. Add the `bucket4j-core` dependency to `build.gradle.kts` (if not already present; if you cannot modify `build.gradle.kts`, output a comment requesting the dependency).
2. Create `TenantRateLimiterService` in `com.vantage.core.ratelimiter`.
3. Maintain a `ConcurrentHashMap<String, Bucket>` where the key is the `tenantId`.
4. Configure the bucket to allow 100 requests per minute (refill greedily: 100 tokens per 60 seconds).

### 2. Rate Limiter Interceptor
1. Create `RateLimitInterceptor` in `com.vantage.core.ratelimiter` implementing `HandlerInterceptor`.
2. In the `preHandle` method:
   - Extract the `tenantId` from `TenantContext.getTenantId()`.
   - If `tenantId` is null (e.g., public endpoint), allow the request.
   - Retrieve or create the bucket for the tenant.
   - Call `bucket.tryConsumeAndReturnRemaining(1)`.
   - If consumption fails, throw a `RateLimitExceededException` or manually set the response status to `429`, add the `Retry-After` header (calculated from the bucket configuration), and return `false`.

### 3. WebMvc Configuration
1. Create or update `WebMvcConfig` in `com.vantage.core.config` implementing `WebMvcConfigurer`.
2. Override `addInterceptors` to add the `RateLimitInterceptor`.
3. Apply the interceptor to `/api/**` paths.
4. Exclude `/api/v1/vendors/register` and `/actuator/**` paths from rate limiting.

### 4. Exception Handling
1. If you threw a `RateLimitExceededException`, add a handler in `GlobalExceptionHandler` to catch it and return a `429 Too Many Requests` status with an `ErrorResponse` payload, including the `Retry-After` header.

### 5. Integration Testing (Testcontainers)
1. Create `RateLimitIT` in `backend/src/test/java/com/vantage/core/ratelimiter/`.
2. Setup: Register a vendor and obtain a JWT.
3. Use a loop to hit the `GET /api/v1/products` endpoint 101 times rapidly.
4. Verify the first 100 requests return `200 OK`.
5. Verify the 101st request returns `429 Too Many Requests`.
6. Verify the `Retry-After` header is present in the 429 response.

## Target File Paths
- `backend/src/main/java/com/vantage/core/ratelimiter/TenantRateLimiterService.java`
- `backend/src/main/java/com/vantage/core/ratelimiter/RateLimitInterceptor.java`
- `backend/src/main/java/com/vantage/core/ratelimiter/RateLimitExceededException.java`
- `backend/src/main/java/com/vantage/core/config/WebMvcConfig.java`
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java` (Modify to handle 429)
- `backend/src/test/java/com/vantage/core/ratelimiter/RateLimitIT.java`
