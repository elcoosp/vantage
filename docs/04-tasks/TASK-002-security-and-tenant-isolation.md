# TASK-002: Implement Multi-Tenant Security & Tenant Isolation

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Multi-Tenancy and Isolation)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 1.1: Vendor Registration, Story 1.2: Tenant Isolation Filter)

## Objective
Implement Spring Security with JWT authentication. Create a security filter that extracts the tenant ID from the JWT and populates the `TenantContext`. Implement a Hibernate filter activation mechanism to enforce data isolation at the ORM level for every request. Finally, implement the Vendor Registration endpoint.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/security/`, `backend/src/main/java/com/vantage/vendor/`, and `backend/src/test/java/com/vantage/`.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other domain modules.

## Context Files to Inject (Dispatch Script: Read these files)
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `VendorRegistrationRequest`, `AuthResponse`, `/api/v1/vendors/register`)
- `docs/02-contracts/03-database-schema.md` (Reference: `vendors` table schema)
- `backend/src/main/java/com/vantage/core/domain/BaseTenantEntity.java`
- `backend/src/main/java/com/vantage/core/tenant/TenantContext.java`

## Acceptance Criteria

### 1. Security Configuration
1. Create `SecurityConfig` in `com.vantage.core.security` configuring Spring Security.
2. Disable CSRF (stateless REST API).
3. Permit `/api/v1/vendors/register` and actuator health endpoints.
4. All other requests must be authenticated.
5. Add a custom `TenantSecurityFilter` before `UsernamePasswordAuthenticationFilter`.

### 2. JWT & Tenant Context Filter
1. Create `JwtService` in `com.vantage.core.security` to generate and parse JWTs. Use the `io.jsonwebtoken` (jjwt) library. The JWT must contain a `tenant_id` claim.
2. Create `TenantSecurityFilter` extending `OncePerRequestFilter`:
   - Extract the `Authorization: Bearer <token>` header.
   - Parse the JWT and extract the `tenant_id` claim.
   - Populate `TenantContext.setTenantId(tenantId)`.
   - **CRITICAL:** In the `finally` block of the filter, call `TenantContext.clear()` to prevent thread leakage (especially important for Java 21 Virtual Threads).

### 3. Hibernate Filter Activation
1. Create `TenantFilterActivator` in `com.vantage.core.security` implementing `OncePerRequestFilter`.
2. This filter must run after `TenantSecurityFilter`.
3. It must obtain the current Hibernate `Session` via `EntityManager.unwrap(Session.class)`.
4. If `TenantContext.getTenantId()` is not null, it must enable the filter: `session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.getTenantId())`.

### 4. Vendor Registration
1. Create `Vendor` entity extending `BaseTenantEntity` in `com.vantage.vendor.domain` matching the `vendors` schema.
2. Create `VendorRepository` extending `JpaRepository`.
3. Create `VendorController` in `com.vantage.vendor.ui` exposing `POST /api/v1/vendors/register`.
4. Create `VendorService` in `com.vantage.vendor.app`:
   - Hash the password using `BCryptPasswordEncoder`.
   - Generate a UUID for the `id` and `tenant_id`.
   - Save the vendor.
   - Generate a JWT containing the `tenant_id` claim.
   - Return the JWT and tenant ID in the `AuthResponse` format.

### 5. Integration Testing (Testcontainers)
1. Create `VendorRegistrationIT` in `backend/src/test/java/com/vantage/vendor/`.
2. Use Testcontainers (PostgreSQL) to test registration.
3. Verify that the endpoint returns `201` and a valid JWT.
4. Create `TenantIsolationIT` in `backend/src/test/java/com/vantage/core/security/`:
   - Register two vendors (Tenant A, Tenant B).
   - Manually insert a product for Tenant A using native SQL or entity manager.
   - Simulate a request context with Tenant B's JWT.
   - Attempt to query the product. Assert that it returns `EmptyResultDataAccessException` or `null` to prove the Hibernate filter blocked cross-tenant access.

## Target File Paths
- `backend/src/main/java/com/vantage/core/security/SecurityConfig.java`
- `backend/src/main/java/com/vantage/core/security/JwtService.java`
- `backend/src/main/java/com/vantage/core/security/TenantSecurityFilter.java`
- `backend/src/main/java/com/vantage/core/security/TenantFilterActivator.java`
- `backend/src/main/java/com/vantage/vendor/domain/Vendor.java`
- `backend/src/main/java/com/vantage/vendor/domain/VendorRepository.java`
- `backend/src/main/java/com/vantage/vendor/app/VendorService.java`
- `backend/src/main/java/com/vantage/vendor/ui/VendorController.java`
- `backend/src/main/java/com/vantage/vendor/ui/dto/VendorRegistrationRequest.java`
- `backend/src/main/java/com/vantage/vendor/ui/dto/AuthResponse.java`
- `backend/src/test/java/com/vantage/vendor/VendorRegistrationIT.java`
- `backend/src/test/java/com/vantage/core/security/TenantIsolationIT.java`
