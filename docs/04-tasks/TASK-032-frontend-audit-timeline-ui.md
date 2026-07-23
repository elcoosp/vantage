# TASK-032: Implement Frontend Audit Timeline (Time Machine UI)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- Read: `docs/04-tasks/TASK-017-event-sourced-audit-timeline.md` (Backend foundation for this UI)

## Objective
Implement a "Git-style" timeline UI in the React 19 frontend that visualizes the immutable event-sourced audit log for an Order. When a vendor clicks an order, they should see a vertical timeline of every state change (Created, Validated, PaId, Cancelled) with color-coded events and expandable JSON diffs, proving full operational visibility.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `AuditEventResponse`)
- `frontend/src/features/orders/OrdersTable.tsx`
- `frontend/src/lib/api.ts`

## Acceptance Criteria

### 1. API Integration
1. Create `frontend/src/features/audit/useOrderAudit.ts` custom hook.
2. Use `@tanstack/react-query` to call `GET /api/v1/audit/orders/{orderId}`.
3. The hook should fetch data on demand when a specific order is selected.

### 2. Timeline Component
1. Create `frontend/src/features/audit/AuditTimeline.tsx`.
2. Render a vertical timeline using pure CSS and Tailwind (or a lightweight library like `react-chrono` if it fits the dark theme well, but custom CSS is preferred to show frontend skills).
3. Each event must be a node on the timeline.
4. Color-code the nodes:
   - `ORDER_CREATED` (Blue)
   - `INVENTORY_RESERVED` (Green)
   - `PAYMENT_SUCCEEDED` (Green)
   - `PAYMENT_FAILED` / `ORDER_CANCELLED` (Red)
   - `INVENTORY_RELEASED` (Orange)
5. Display the `eventType`, `createdAt` timestamp, and a button to "View Details".

### 3. JSON Diff Viewer
1. When a user clicks "View Details" on a timeline node, expand a section below it.
2. Render the `payload` JSON in a syntax-highlighted code block.
3. If a diff library (like `react-diff-viewer`) is used, show the difference between the previous event's payload and the current one. Otherwise, just pretty-print the JSON with syntax highlighting.

### 4. UI Integration
1. Create `frontend/src/features/audit/OrderDetailModal.tsx`.
2. This modal should open when a user clicks an order row in the `OrdersTable` (from Task-022).
3. The modal should display the order summary at the top and the `AuditTimeline` below it.

### 5. Verification (Output Instructions)
- Provide the terminal commands to install any new npm dependencies (e.g., `react-diff-viewer-continued`).
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/features/audit/AuditTimeline.tsx`
- `frontend/src/features/audit/OrderDetailModal.tsx`
- `frontend/src/features/audit/useOrderAudit.ts`
