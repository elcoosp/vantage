# TASK-005: Implement Inventory Reservation Consumer & Idempotency

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Distributed Order Engine)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 3.2: Saga Orchestration & Compensating Transactions)

## Objective
Implement the RabbitMQ consumer in the `inventory` module to listen for `OrderCreatedEvent`. The consumer must decrement stock and emit an `InventoryReservedEvent`. To guarantee exactly-once processing, the consumer must use a `processed_events` table to track consumed event IDs.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/inventory/`, `backend/src/main/java/com/vantage/core/messaging/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `OrderCreatedEvent`, `InventoryReservedEvent`, `InventoryReleasedEvent`, `EventMetadata`)
- `docs/02-contracts/03-database-schema.md` (Reference: `processed_events` table)
- `backend/src/main/java/com/vantage/inventory/domain/Inventory.java`
- `backend/src/main/java/com/vantage/inventory/domain/InventoryRepository.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxRepository.java`

## Acceptance Criteria

### 1. Processed Events Table & Entity
1. Create `ProcessedEvent` entity in `com.vantage.core.messaging.domain` matching the `processed_events` schema.
   - Fields: `eventId` (UUID, PK), `tenantId`, `processedAt`.
2. Create `ProcessedEventRepository` extending `JpaRepository`.

### 2. Inventory Reservation Consumer
1. Create `InventoryOrderConsumer` in `com.vantage.inventory.messaging`.
2. Listen to the `vantage.order.events` queue for `OrderCreatedEvent`.
3. Extract the `eventId` from the message metadata. Check `ProcessedEventRepository` to see if this event was already processed. If yes, ACK the message and return (idempotency).
4. If not processed, attempt to decrement the inventory for the requested `productId` and `quantity`.
5. **Concurrency Handling:** If `quantity` exceeds available stock, or an `ObjectOptimisticLockingFailureException` is thrown, log the failure and emit an `InventoryReservationFailedEvent` to the outbox.
6. If successful, emit an `InventoryReservedEvent` to the outbox.
7. In the same database transaction, save the `eventId` to the `processed_events` table to mark it as processed.
8. ACK the RabbitMQ message only after the database transaction commits successfully.

### 3. Outbox Event Payload Mapping
1. The `InventoryReservedEvent` and `InventoryReservationFailedEvent` payloads must conform to the AsyncAPI specification.
2. They must include the original `orderId`, `productId`, `reservedQuantity` (or failed quantity), and the `tenantId` in the metadata.

### 4. Integration Testing (Testcontainers)
1. Create `InventoryConsumerIT` in `backend/src/test/java/com/vantage/inventory/`.
2. Setup: Register vendor, create product, initialize inventory to 10.
3. Test 1 (Success): Publish an `OrderCreatedEvent` for quantity 5. Verify inventory drops to 5. Verify `InventoryReservedEvent` is published to RabbitMQ. Verify `processed_events` table contains the event ID.
4. Test 2 (Idempotency): Republish the exact same `OrderCreatedEvent` for quantity 5. Verify inventory remains at 5. Verify no new `InventoryReservedEvent` is published.
5. Test 3 (Failure): Publish an `OrderCreatedEvent` for quantity 100 (exceeds stock). Verify inventory remains unchanged. Verify `InventoryReservationFailedEvent` is published.

## Target File Paths
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`
- `backend/src/main/java/com/vantage/inventory/messaging/InventoryOrderConsumer.java`
- `backend/src/main/java/com/vantage/inventory/app/event/InventoryReservedPayload.java`
- `backend/src/main/java/com/vantage/inventory/app/event/InventoryReservationFailedPayload.java`
- `backend/src/test/java/com/vantage/inventory/InventoryConsumerIT.java`
