# TASK-016: Implement Frontend Authentication Flow

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 1.1: Vendor Self-Registration)

## Objective
Implement the login and registration screens for the Vantage frontend. Wire these screens to the Zustand auth store and Axios interceptor. Upon successful authentication, the application must redirect the user to the main dashboard and persist their JWT token.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `VendorRegistrationRequest`, `AuthResponse`, `/api/v1/vendors/register`)
- `frontend/src/lib/api.ts`
- `frontend/src/store/authStore.ts`
- `frontend/src/App.tsx`

## Acceptance Criteria

### 1. Authentication Routes & Layout
1. Create an `AuthLayout.tsx` component for unauthenticated pages (centered card, Vantage branding).
2. Create routes `/login` and `/register` in `App.tsx`.
3. Implement a protected route wrapper that redirects to `/login` if the user is not authenticated.

### 2. Registration Page
1. Create `frontend/src/features/auth/RegisterPage.tsx`.
2. Form fields: Email, Password, Store Name, Store Slug.
3. On submit, call `POST /api/v1/vendors/register`.
4. If successful, save the `accessToken` and `tenantId` to the Zustand store and `localStorage`.
5. Redirect to `/dashboard`.
6. Display validation errors (e.g., "Tenant slug already exists") via toast notifications.

### 3. Login Page
1. Create `frontend/src/features/auth/LoginPage.tsx`.
2. Form fields: Email, Password.
3. Create a mock login endpoint in the frontend API client or assume a `POST /api/v1/vendors/login` exists in the backend.
4. If successful, save the `accessToken` and `tenantId` to the Zustand store and `localStorage`.
5. Redirect to `/dashboard`.

### 4. Logout Functionality
1. Add a "Logout" button to the main `Layout.tsx` sidebar/header.
2. On click, clear the auth store, remove the token from `localStorage`, and redirect to `/login`.

### 5. Verification (Output Instructions)
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/components/AuthLayout.tsx`
- `frontend/src/components/ProtectedRoute.tsx`
- `frontend/src/features/auth/RegisterPage.tsx`
- `frontend/src/features/auth/LoginPage.tsx`
- `frontend/src/features/auth/api.ts`
- `frontend/src/App.tsx` (Modify to add routes and protected wrappers)
- `frontend/src/components/Layout.tsx` (Modify to add Logout button)
