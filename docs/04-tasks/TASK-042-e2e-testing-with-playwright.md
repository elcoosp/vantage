# TASK-042: Implement End-to-End (E2E) Testing with Playwright

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Engineering Maturity)
- Read: `docs/00-product/03-epics-and-user-stories.md` (Verify full user journeys)

## Objective
Implement End-to-End (E2E) tests using Playwright to verify the full-stack integration between the React 19 frontend and the Spring Boot backend. The tests must cover critical user journeys: Vendor Registration, Product Creation, Inventory Update (Optimistic Locking), and Order Placement (Saga trigger). This proves to recruiters that the application actually works end-to-end and not just in isolated unit tests.

## Execution Boundaries
- You may ONLY create or modify files inside `frontend/e2e/`, `frontend/playwright.config.ts`, `frontend/package.json` (to add scripts), and `.github/workflows/e2e.yml`.
- DO NOT modify backend source code or frontend feature components.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `frontend/package.json`
- `frontend/src/features/auth/RegisterPage.tsx`
- `frontend/src/features/inventory/InventoryGrid.tsx`

## Acceptance Criteria

### 1. Playwright Setup
1. Add `@playwright/test` to `frontend/package.json` devDependencies.
2. Create `frontend/playwright.config.ts`.
3. Configure the base URL to `http://localhost:5173` (Vite dev server) or `http://localhost:4173` (Vite preview).
4. Configure projects for Chromium, Firefox, and WebKit.

### 2. E2E Test Scenarios
1. Create `frontend/e2e/vendor-journey.spec.ts`.
2. **Test 1: Registration & Onboarding**
   - Navigate to `/register`.
   - Fill in email, password, store name, and slug.
   - Click "Register".
   - Verify redirection to `/dashboard`.
3. **Test 2: Product Creation**
   - Click the `Cmd+K` palette.
   - Select "Add New Product".
   - Fill in the product modal (Name: "E2E Test Mug", Price: "15.00").
   - Submit the form.
   - Verify the product appears in the UI.
4. **Test 3: Inventory Update**
   - Navigate to the Inventory grid.
   - Click "Edit Quantity" for the newly created product.
   - Enter quantity "50".
   - Submit.
   - Verify the UI optimistically updates to 50 and persists after reload.
5. **Test 4: Order Placement**
   - Navigate to the Order placement view (or trigger via API if UI doesn't exist for it).
   - Place an order for the "E2E Test Mug".
   - Verify the order appears in the Orders table with status "CREATED".

### 3. CI Pipeline Integration
1. Create `.github/workflows/e2e.yml`.
2. Trigger on `pull_request`.
3. Steps:
   - Checkout code.
   - Setup Node.js.
   - Start backend and database via Docker Compose (from root `docker-compose.yml`).
   - Wait for backend health check to pass.
   - Install frontend dependencies (`npm ci`).
   - Install Playwright browsers (`npx playwright install --with-deps`).
   - Start the frontend preview server (`npm run preview`).
   - Run Playwright tests (`npx playwright test`).
   - Upload the Playwright HTML report as a CI artifact on failure.

### 4. Verification (Output Instructions)
- Provide the local command to run the E2E tests: `cd frontend && npx playwright test --ui` (for interactive) or `npx playwright test`.

## Target File Paths
- `frontend/playwright.config.ts`
- `frontend/e2e/vendor-journey.spec.ts`
- `frontend/package.json` (Modify to add test scripts)
- `.github/workflows/e2e.yml`
