# TASK-043: Implement Admin Tenant Management UI & Suspension Logic

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 2 - The Platform Admin)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 1.2: Admin Tenant Management)

## Objective
Implement the Platform Admin's Tenant Management dashboard. The Admin must be able to view a list of all registered vendors (tenants), see their status (ACTIVE/SUSPENDED), and toggle their status. When a tenant is suspended, their JWT must be invalidated, and they must be blocked from logging in or making API requests, proving robust SaaS platform governance.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/admin/`, `backend/src/main/java/com/vantage/vendor/`, `backend/src/main/java/com/vantage/core/security/`, `frontend/src/features/admin/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or unrelated modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/03-database-schema.md` (Reference: `vendors` table)
- `backend/src/main/java/com/vantage/vendor/domain/Vendor.java`
- `backend/src/main/java/com/vantage/vendor/domain/VendorRepository.java`
- `backend/src/main/java/com/vantage/core/security/JwtService.java`
- `backend/src/main/java/com/vantage/core/security/TenantSecurityFilter.java`

## Acceptance Criteria

### 1. Backend Tenant Management Endpoints
1. Create `AdminTenantController` in `com.vantage.core.admin.ui`.
2. Expose `GET /api/v1/admin/tenants`: Returns a list of all vendors (id, storeName, tenantSlug, email, status). This endpoint must bypass the tenant filter to see all records.
3. Expose `PUT /api/v1/admin/tenants/{tenantId}/status`: Accepts a body `{ "status": "SUSPENDED" }` or `{ "status": "ACTIVE" }`.
4. Update the `VendorService` or create an `AdminTenantService` to handle the status update.

### 2. Security & Suspension Enforcement
1. Modify `Vendor` entity to include a `status` enum field (`ACTIVE`, `SUSPENDED`) if not already present.
2. Modify `TenantSecurityFilter` (or `JwtService`): When validating the JWT, check the vendor's status in the database (or a cached version of it). If the vendor is `SUSPENDED`, clear the `TenantContext` and return `403 Forbidden` with a message "Tenant account is suspended."
3. Modify the Login/Registration flow: If a vendor attempts to log in or register while suspended, deny the action.

### 3. Frontend Admin Tenant Dashboard
1. Create `frontend/src/features/admin/TenantManagement.tsx`.
2. Fetch all tenants using React Query (`GET /api/v1/admin/tenants`).
3. Render a table displaying Tenant Name, Slug, Email, Status, and an "Actions" column.
4. If the tenant is `ACTIVE`, show a "Suspend" button (Red).
5. If the tenant is `SUSPENDED`, show a "Reactivate" button (Green).
6. Clicking the button calls the `PUT /api/v1/admin/tenants/{tenantId}/status` endpoint and invalidates the React Query cache to refresh the list.

### 4. Verification (Output Instructions)
- Document how to test: Register two tenants (A and B). Log in as Admin, suspend Tenant B. Attempt to make an API call as Tenant B and verify a `403 Forbidden` is returned.

## Target File Paths
- `backend/src/main/java/com/vantage/core/admin/ui/AdminTenantController.java`
- `backend/src/main/java/com/vantage/core/admin/app/AdminTenantService.java`
- `backend/src/main/java/com/vantage/vendor/domain/VendorStatus.java`
- `backend/src/main/java/com/vantage/core/security/TenantSecurityFilter.java` (Modify)
- `frontend/src/features/admin/TenantManagement.tsx`
- `frontend/src/features/admin/useTenants.ts`
