# TASK-004: Implement Transactional Order Creation (Outbox Pattern)

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Distributed Order Engine)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 3.1: Transactional Order Creation)

## Objective
Implement the `order` module and the Transactional Outbox pattern. The system must save the `Order` and an `OutboxEvent` in the exact same database transaction to solve the dual-write problem. A scheduled poller must read unpublished events and publish them to RabbitMQ with publisher confirms.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/order/`, `backend/src/main/java/com/vantage/core/messaging/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `OrderCreatedEvent`, `OrderCreatedPayload`)
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `OrderRequest`, `OrderResponse`, `POST /api/v1/orders`)
- `docs/02-contracts/03-database-schema.md` (Reference: `orders` and `outbox_events` tables)
- `backend/src/main/java/com/vantage/core/domain/BaseTenantEntity.java`

## Acceptance Criteria

### 1. Order Module Implementation
1. Create `Order` entity extending `BaseTenantEntity` matching the `orders` schema. Status must default to `CREATED`.
2. Create `OrderRepository` extending `JpaRepository`.
3. Create `OrderController` exposing `POST /api/v1/orders`.
   - Must return `202 Accepted` with the `OrderResponse` payload.

### 2. Outbox Infrastructure
1. Create `OutboxEvent` entity in `com.vantage.core.messaging.domain` matching the `outbox_events` schema.
   - Fields: `id` (UUID), `tenantId`, `aggregateType`, `aggregateId`, `eventType`, `payload` (String/JSON), `status` (Enum: `PENDING`, `PUBLISHED`), `createdAt`, `publishedAt`.
2. Create `OutboxRepository` extending `JpaRepository`.
3. Create `OrderService` in `com.vantage.order.app`:
   - Method `createOrder` must be annotated with `@Transactional`.
   - Save the `Order` entity.
   - Construct an `OrderCreatedPayload` (matching the AsyncAPI spec) and serialize it to JSON.
   - Save an `OutboxEvent` with `aggregateType` "ORDER", `aggregateId` (Order ID), `eventType` "OrderCreatedEvent", and the JSON payload.

### 3. RabbitMQ Configuration & Publisher Confirms
1. Create `RabbitMQConfig` in `com.vantage.core.messaging.config`.
2. Configure the exchange (`vantage.events`), queue (`vantage.order.events`), and routing key (`order.created`).
3. Enable publisher confirms and returns in the `RabbitTemplate` configuration.
4. Implement a `RabbitPublisherConfirmCallback` to handle ACKs/NACKs.

### 4. Outbox Poller
1. Create `OutboxPoller` in `com.vantage.core.messaging.app`:
   - Annotate a method with `@Scheduled(fixedDelay = 2000)` (run every 2 seconds).
   - Query the `outbox_events` table for events with `status = 'PENDING'`, ordered by `created_at`.
   - For each event, publish to RabbitMQ using the `RabbitTemplate`.
   - Only upon receiving a publisher ACK, update the event status to `PUBLISHED` and set `publishedAt`. If NACKed, leave as `PENDING` for the next retry.

### 5. Integration Testing (Testcontainers)
1. Create `OrderOutboxIT` in `backend/src/test/java/com/vantage/order/`.
2. Use Testcontainers for PostgreSQL and RabbitMQ.
3. Register a vendor, create a product, and call `POST /api/v1/orders`.
4. Verify the `orders` table contains the record.
5. Verify the `outbox_events` table contains a `PENDING` record in the same transaction (query immediately after API call before the poller runs).
6. Await at most 5 seconds for the poller to publish the event.
7. Verify the `outbox_events` status is updated to `PUBLISHED`.
8. Verify the RabbitMQ queue receives the exact `OrderCreatedEvent` JSON payload.

## Target File Paths
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/order/domain/OrderStatus.java`
- `backend/src/main/java/com/vantage/order/domain/OrderRepository.java`
- `backend/src/main/java/com/vantage/order/app/OrderService.java`
- `backend/src/main/java/com/vantage/order/ui/OrderController.java`
- `backend/src/main/java/com/vantage/order/ui/dto/OrderRequest.java`
- `backend/src/main/java/com/vantage/order/ui/dto/OrderResponse.java`
- `backend/src/main/java/com/vantage/order/app/event/OrderCreatedPayload.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxStatus.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxRepository.java`
- `backend/src/main/java/com/vantage/core/messaging/config/RabbitMQConfig.java`
- `backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java`
- `backend/src/test/java/com/vantage/order/OrderOutboxIT.java`
