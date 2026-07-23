# TASK-020: Implement Developer Portal & API Key Authentication

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 3 - The External Developer)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Understand the need for API integrations)

## Objective
Transform Vantage from a pure UI-driven SaaS into a developer platform. Implement a "Stripe-style" API key system. Vendors can generate API keys from the dashboard. External systems can use these keys via the `X-API-Key` header to authenticate REST API requests instead of using a JWT.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/integration/`, `backend/src/main/java/com/vantage/core/security/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ApiKeyAuth` security scheme)
- `docs/02-contracts/03-database-schema.md`
- `backend/src/main/java/com/vantage/core/security/SecurityConfig.java`
- `backend/src/main/java/com/vantage/core/security/TenantSecurityFilter.java`
- `backend/src/main/java/com/vantage/vendor/domain/Vendor.java`

## Acceptance Criteria

### 1. API Key Infrastructure
1. Create `ApiKey` entity in `com.vantage.integration.domain` extending `BaseTenantEntity`.
   - Fields: `id` (UUID), `name` (String), `keyHash` (String), `keyPrefix` (String), `lastUsedAt` (Instant), `revoked` (boolean).
2. Create `ApiKeyRepository` extending `JpaRepository`.

### 2. API Key Generation Endpoint
1. Create `ApiKeyController` in `com.vantage.integration.ui` exposing `POST /api/v1/api-keys`.
2. Request body requires a `name` (e.g., "ERP Integration").
3. The service must generate a secure random API key string (e.g., `vnt_live_` + 32 random alphanumeric characters).
4. Store a BCrypt hash of the key in `keyHash`. Store the first 12 characters in `keyPrefix` for display purposes (e.g., `vnt_live_abc...`).
5. Return the plaintext key in the response exactly once. Subsequent queries must only return the prefix.

### 3. Security Filter Integration
1. Update `TenantSecurityFilter` in `com.vantage.core.security` to check for the `X-API-Key` header if the `Authorization: Bearer` header is missing.
2. If `X-API-Key` is present:
   - Query the `ApiKeyRepository` for all active keys (or use a cache).
   - Iterate and use `BCryptPasswordEncoder.matches()` to find the valid key.
   - If matched, extract the `tenantId` from the `ApiKey` entity, populate `TenantContext`, and update the `lastUsedAt` timestamp.
3. Ensure the Spring Security `SecurityFilterChain` permits requests with a valid API key.

### 4. Integration Testing (Testcontainers)
1. Create `ApiKeyAuthenticationIT` in `backend/src/test/java/com/vantage/integration/`.
2. Setup: Register a vendor, generate an API key.
3. Test 1 (Success): Call `GET /api/v1/products` with the `X-API-Key` header. Assert `200 OK`.
4. Test 2 (Invalid Key): Call `GET /api/v1/products` with a fake API key. Assert `401 Unauthorized`.
5. Test 3 (Revocation): Revoke the key via `DELETE /api/v1/api-keys/{id}`. Call `GET /api/v1/products` with the revoked key. Assert `401 Unauthorized`.

## Target File Paths
- `backend/src/main/java/com/vantage/integration/domain/ApiKey.java`
- `backend/src/main/java/com/vantage/integration/domain/ApiKeyRepository.java`
- `backend/src/main/java/com/vantage/integration/app/ApiKeyService.java`
- `backend/src/main/java/com/vantage/integration/ui/ApiKeyController.java`
- `backend/src/main/java/com/vantage/integration/ui/dto/ApiKeyResponse.java`
- `backend/src/main/java/com/vantage/core/security/TenantSecurityFilter.java` (Modify to support X-API-Key)
- `backend/src/test/java/com/vantage/integration/ApiKeyAuthenticationIT.java`
