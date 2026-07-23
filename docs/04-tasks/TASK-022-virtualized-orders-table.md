# TASK-022: Implement High-Performance Virtualized Orders Table

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- Read: `docs/00-product/03-epics-and-user-stories.md` (Understand the need for power-user UI handling large datasets)

## Objective
Implement a high-performance, virtualized data grid for the Order Search page using TanStack Table and TanStack Virtual. This ensures the UI can render 10,000+ orders at a smooth 60fps by only rendering the DOM nodes currently visible in the scroll viewport, proving senior-level frontend rendering optimization skills.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `OrderSearchResultResponse`)
- `frontend/src/components/Layout.tsx`
- `frontend/src/lib/api.ts`

## Acceptance Criteria

### 1. Dependencies & Setup
1. Install `@tanstack/react-table` and `@tanstack/react-virtual` in the `frontend/` directory.
2. Create a new route `/orders` in the application router.

### 2. Data Fetching
1. Create `frontend/src/features/orders/useOrders.ts` custom hook.
2. Use `@tanstack/react-query` to call `GET /api/v1/orders/search`.
3. Configure the hook to fetch a large page size (e.g., `size=10000`) to simulate a heavy dataset for virtualization.

### 3. Virtualized Table Implementation
1. Create `frontend/src/features/orders/OrdersTable.tsx`.
2. Use `useReactTable` from TanStack Table to define columns: Order ID, Product Name, Status, Quantity, Created At.
3. Use `useVirtualizer` (from `@tanstack/react-virtual`) with the `getCoreRowModel`.
4. The virtualizer must dynamically calculate the total height of all rows and only render the slice of rows currently visible in the scroll container.
5. Render a sticky header row that remains visible during vertical scrolling.

### 4. UI Polish
1. Apply Tailwind CSS classes for a modern, dark-mode enterprise look.
2. Add alternating row colors (zebra striping) that correctly track with the virtual scroll index.
3. Display a loading skeleton state while data is fetching.
4. Add a custom scrollbar style to match the dark theme.

### 5. Verification (Output Instructions)
- Provide the terminal commands to install the new npm dependencies.
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/features/orders/OrdersPage.tsx`
- `frontend/src/features/orders/OrdersTable.tsx`
- `frontend/src/features/orders/useOrders.ts`
