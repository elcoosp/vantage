# TASK-012: Implement React 19 Dashboard, Optimistic Updates, and Command Palette

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 2.1: Optimistic Stock Updates, Story 6.2: Power User Command Palette)

## Objective
Implement the core React 19 frontend dashboard for the Vendor persona. This includes setting up the data fetching layer (React Query), a `Cmd+K` command palette for power-user navigation, and an inventory management grid that uses React 19's `useOptimistic` hook to instantly reflect stock changes without waiting for the network, gracefully rolling back on a `409 Conflict`.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ProductResponse`, `InventoryResponse`, `InventoryUpdateRequest`, `If-Match` header, `409 Conflict` response)
- `frontend/package.json`
- `frontend/src/App.tsx`

## Acceptance Criteria

### 1. API & State Management Setup
1. Create an Axios instance (`frontend/src/lib/api.ts`) configured with a base URL of `http://localhost:8080/api/v1`.
2. Implement an Axios interceptor that injects the `Authorization: Bearer <token>` header from `localStorage`.
3. Setup `QueryClientProvider` in `main.tsx`.
4. Create a Zustand store (`frontend/src/store/authStore.ts`) to manage the JWT token and authentication state.

### 2. Application Layout
1. Create a main layout component (`frontend/src/components/Layout.tsx`) with a dark-mode sidebar and top header.
2. Implement routing using `react-router-dom` for the Dashboard, Products, and Inventory pages.

### 3. Command Palette (Cmd+K)
1. Install the `cmdk` library.
2. Create `frontend/src/components/CommandPalette.tsx`.
3. Bind the palette to the `Cmd+K` (or `Ctrl+K`) keyboard shortcut.
4. Include commands:
   - "Go to Dashboard"
   - "Go to Inventory"
   - "Add New Product" (opens a modal)
   - "Update Stock" (opens a modal)
5. The palette must overlay the application with a semi-transparent backdrop.

### 4. Inventory Grid with Optimistic Updates
1. Create `frontend/src/features/inventory/InventoryGrid.tsx`.
2. Fetch inventory data using React Query (`useQuery`).
3. Render the data in a table (Product Name, Quantity, Version).
4. Add an "Edit Quantity" button to each row that opens an input field.
5. When the user submits a new quantity:
   - Use React 19's `useOptimistic` hook to instantly update the quantity in the UI.
   - Fire a `useMutation` calling `PUT /api/v1/inventory/{productId}` with the `If-Match: <version>` header.
6. **Conflict Handling:** If the API returns `409 Conflict`, the optimistic update must be rolled back, and a toast notification must appear saying "Conflict: Another user modified this item. Please refresh."
7. If successful, invalidate the React Query cache to fetch the authoritative data with the new `version`.

### 5. Verification (Output Instructions)
- Provide the terminal commands to install the new npm dependencies (`cmdk`, `zustand`, `@tanstack/react-query`, `axios`, `react-router-dom`, `react-hot-toast`).
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/lib/api.ts`
- `frontend/src/store/authStore.ts`
- `frontend/src/main.tsx`
- `frontend/src/App.tsx`
- `frontend/src/components/Layout.tsx`
- `frontend/src/components/CommandPalette.tsx`
- `frontend/src/features/inventory/InventoryGrid.tsx`
- `frontend/src/features/inventory/InventoryEditForm.tsx`
