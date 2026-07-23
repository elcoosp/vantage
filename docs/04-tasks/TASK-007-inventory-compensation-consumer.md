# TASK-007: Implement Inventory Compensation Consumer

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Distributed Order Engine, Compensating Transactions)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 3.2: Saga Compensation)

## Objective
Implement the RabbitMQ consumer in the `inventory` module to listen for `InventoryReleasedEvent`. This is the compensating transaction that restores stock when a payment fails. It must also guarantee idempotency using the `processed_events` table.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/inventory/` and its corresponding test directory.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `InventoryReleasedEvent`, `InventoryReleasedPayload`)
- `backend/src/main/java/com/vantage/inventory/domain/Inventory.java`
- `backend/src/main/java/com/vantage/inventory/domain/InventoryRepository.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`

## Acceptance Criteria

### 1. Compensation Consumer Implementation
1. Create `InventoryCompensationConsumer` in `com.vantage.inventory.messaging`.
2. Listen to the `vantage.inventory.events` queue for `InventoryReleasedEvent`.
3. Extract the `eventId` from the message metadata. Check `ProcessedEventRepository` for idempotency. If already processed, ACK and return.
4. If not processed, find the `Inventory` record by `productId` and `tenantId`.
5. Increment the inventory `quantity` by the `releasedQuantity` from the event payload.
6. Save the updated `Inventory` entity. (Rely on `@Version` for optimistic locking during compensation).
7. In the same database transaction, save the `eventId` to the `processed_events` table.
8. ACK the RabbitMQ message only after the database transaction commits successfully.

### 2. Integration Testing (Testcontainers)
1. Create `InventoryCompensationIT` in `backend/src/test/java/com/vantage/inventory/`.
2. Setup: Register vendor, create product, initialize inventory to 10.
3. Manually publish an `InventoryReleasedEvent` for quantity 4.
4. Await at most 5 seconds.
5. Verify the `inventory` table shows quantity 14.
6. Verify the `processed_events` table contains the event ID.
7. Republish the same `InventoryReleasedEvent`. Verify quantity remains 14 and no duplicate processing occurs.

## Target File Paths
- `backend/src/main/java/com/vantage/inventory/messaging/InventoryCompensationConsumer.java`
- `backend/src/test/java/com/vantage/inventory/InventoryCompensationIT.java`
