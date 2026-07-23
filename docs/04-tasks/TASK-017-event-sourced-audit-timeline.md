# TASK-017: Implement Event-Sourced Audit Timeline (Time Machine)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Understand the need for operational visibility)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 5.1 Module Responsibilities)

## Objective
Implement an immutable, queryable audit log for the `Order` entity. Instead of relying solely on `updated_at` columns, the system will capture every state change and field update as an immutable event in an `entity_events` table. This creates a "Git-style" timeline for recruiters to view the exact lifecycle of an order, including compensating transactions.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/audit/`, `backend/src/main/java/com/vantage/order/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml`
- `docs/02-contracts/03-database-schema.md`
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/order/domain/OrderStatus.java`

## Acceptance Criteria

### 1. Audit Infrastructure
1. Create `EntityEvent` entity in `com.vantage.core.audit.domain`.
   - Fields: `id` (UUID), `tenantId` (UUID), `aggregateType` (String), `aggregateId` (UUID), `eventType` (String), `payload` (JSONB/String), `createdAt` (Instant).
2. Create `EntityEventRepository` extending `JpaRepository`.

### 2. Hibernate Event Listener (Automatic Capture)
1. Create `AuditEntityEventListener` in `com.vantage.core.audit.infrastructure` implementing Hibernate's `PostInsertEventListener` and `PostUpdateEventListener`.
2. Register the listener in a `@Component` implementing `Integrator` or via `@Bean` registering an `EventListenerRegistry`.
3. When an `Order` entity is inserted or updated, the listener must:
   - Capture the current state (fields and values) as a JSON payload.
   - Determine the `eventType` (e.g., `ORDER_CREATED`, `ORDER_UPDATED`).
   - Save an `EntityEvent` record in the same transaction.

### 3. REST API for Timeline
1. Create `AuditController` in `com.vantage.core.audit.ui` exposing `GET /api/v1/audit/orders/{orderId}`.
2. The endpoint must query `EntityEventRepository` for all events where `aggregateId` equals the path variable.
3. Return a list of `AuditEventResponse` objects sorted by `createdAt` ascending.

### 4. Integration Testing (Testcontainers)
1. Create `AuditTimelineIT` in `backend/src/test/java/com/vantage/core/audit/`.
2. Setup: Register a vendor, create a product, initialize inventory.
3. Place an order (triggers `ORDER_CREATED`).
4. Simulate payment failure (triggers `ORDER_UPDATED` to `CANCELLED`).
5. Call `GET /api/v1/audit/orders/{orderId}`.
6. Verify the response contains exactly two events.
7. Verify the first event type is `ORDER_CREATED` and the second is `ORDER_UPDATED`.
8. Verify the JSON payload of the second event reflects the `CANCELLED` status.

## Target File Paths
- `backend/src/main/java/com/vantage/core/audit/domain/EntityEvent.java`
- `backend/src/main/java/com/vantage/core/audit/domain/EntityEventRepository.java`
- `backend/src/main/java/com/vantage/core/audit/infrastructure/AuditEntityEventListener.java`
- `backend/src/main/java/com/vantage/core/audit/infrastructure/AuditIntegrator.java`
- `backend/src/main/java/com/vantage/core/audit/ui/AuditController.java`
- `backend/src/main/java/com/vantage/core/audit/ui/dto/AuditEventResponse.java`
- `backend/src/test/java/com/vantage/core/audit/AuditTimelineIT.java`
