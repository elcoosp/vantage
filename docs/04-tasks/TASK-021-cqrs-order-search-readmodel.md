# TASK-021: Implement CQRS Read Model for Order Search

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand modular boundaries and data flow)
- Read: `docs/00-product/01-vision-and-personas.md` (Understand the need for high-performance vendor dashboards)

## Objective
Implement Command Query Responsibility Segregation (CQRS) for the Order module. Create a denormalized `order_search_view` table that acts as a read model. Update this projection asynchronously by listening to domain events (`OrderCreatedEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent`). The search endpoint will query this read model instead of performing expensive joins on the transactional `orders` table.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/order/`, `backend/src/main/java/com/vantage/core/messaging/`, `backend/src/main/resources/db/migration/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other domain modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `OrderCreatedEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent`)
- `docs/02-contracts/03-database-schema.md` (Reference: `orders` table)
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`

## Acceptance Criteria

### 1. Database Migration (Flyway)
1. Create a new Flyway migration script (e.g., `V3__create_order_search_view.sql`).
2. Create an `order_search_view` table with the following columns: `order_id` (UUID, PK), `tenant_id` (UUID), `product_name` (VARCHAR), `status` (VARCHAR), `quantity` (INTEGER), `created_at` (TIMESTAMPTZ).
3. Create an index on `tenant_id` and `status`.

### 2. Read Model Projection Entity & Repository
1. Create `OrderSearchView` entity in `com.vantage.order.query.domain` mapped to the `order_search_view` table. This entity must NOT extend `BaseEntity` or `BaseTenantEntity` as it is a pure read model.
2. Create `OrderSearchViewRepository` extending `JpaRepository`.

### 3. Event Projectors (Consumers)
1. Create `OrderSearchProjector` in `com.vantage.order.query.messaging`.
2. Listen to `vantage.order.events` and `vantage.payment.events` for `OrderCreatedEvent`, `PaymentSucceededEvent`, and `PaymentFailedEvent`.
3. For `OrderCreatedEvent`:
   - Check `ProcessedEventRepository` for idempotency.
   - Fetch the `Product` name (via `ProductRepository` or include it in the event payload to avoid cross-module coupling). *Note: To maintain strict Modulith boundaries, assume `productName` is added to the `OrderCreatedPayload` in AsyncAPI spec.*
   - Insert a new row into `order_search_view` with status `CREATED`.
   - Save the event ID to `processed_events`.
4. For `PaymentSucceededEvent`:
   - Check idempotency.
   - Update the `status` of the corresponding row to `PAID`.
   - Save the event ID.
5. For `PaymentFailedEvent`:
   - Check idempotency.
   - Update the `status` to `CANCELLED`.
   - Save the event ID.

### 4. Query API
1. Create `OrderQueryController` in `com.vantage.order.query.ui` exposing `GET /api/v1/orders/search`.
2. The endpoint must accept optional `status` and `page`/size` query parameters.
3. Query the `OrderSearchViewRepository` filtered by `tenant_id` (using `TenantContext`).
4. Return a paginated list of `OrderSearchResultResponse` DTOs.

### 5. Integration Testing (Testcontainers)
1. Create `OrderSearchCqrsIT` in `backend/src/test/java/com/vantage/order/query/`.
2. Setup: Register vendor, create product, place order.
3. Await event processing.
4. Call `GET /api/v1/orders/search`. Verify the order appears with status `CREATED` and the correct `productName`.
5. Simulate payment success. Await processing.
6. Call the search endpoint again. Verify the status is now `PAID`.

## Target File Paths
- `backend/src/main/resources/db/migration/V3__create_order_search_view.sql`
- `backend/src/main/java/com/vantage/order/query/domain/OrderSearchView.java`
- `backend/src/main/java/com/vantage/order/query/domain/OrderSearchViewRepository.java`
- `backend/src/main/java/com/vantage/order/query/messaging/OrderSearchProjector.java`
- `backend/src/main/java/com/vantage/order/query/ui/OrderQueryController.java`
- `backend/src/main/java/com/vantage/order/query/ui/dto/OrderSearchResultResponse.java`
- `backend/src/test/java/com/vantage/order/query/OrderSearchCqrsIT.java`
