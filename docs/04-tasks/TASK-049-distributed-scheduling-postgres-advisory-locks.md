# TASK-049: Implement Distributed Scheduling with PostgreSQL Advisory Locks

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 4. Container View - Horizontal Scaling)
- Read: `docs/04-tasks/TASK-004-transactional-outbox-and-order-creation.md` (The Outbox Poller)

## Objective
Prepare the Vantage platform for horizontal scaling. When multiple instances of the backend are running (e.g., on Render), scheduled tasks like the `OutboxPoller` will execute simultaneously, causing duplicate RabbitMQ messages and race conditions. Implement distributed locking using PostgreSQL Advisory Locks to ensure only one instance runs the scheduled task at any given time, demonstrating elite-level distributed systems and database internals knowledge without requiring external tools like Redis.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/messaging/`, `backend/src/main/java/com/vantage/core/db/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/OutboxRepository.java`

## Acceptance Criteria

### 1. Advisory Lock Infrastructure
1. Create `DistributedLockService` in `com.vantage.core.db`.
2. Implement a method `boolean tryAcquireLock(String lockName)`.
   - Use a native SQL query to acquire a Postgres session-level advisory lock: `SELECT pg_try_advisory_lock(hashtext(:lockName))`.
   - Use `hashtext` to convert the string lock name to an int64 (the required input for `pg_try_advisory_lock`).
3. Implement a method `void releaseLock(String lockName)`.
   - Use `SELECT pg_advisory_unlock(hashtext(:lockName))`.
4. Handle the `JpaSystemException` or `SQLException` gracefully if the lock cannot be acquired.

### 2. Outbox Poller Integration
1. Modify `OutboxPoller` in `com.vantage.core.messaging.app`.
2. At the beginning of the `@Scheduled` method, call `distributedLockService.tryAcquireLock("outbox_poller")`.
3. If `false` is returned, log "Another instance holds the lock. Skipping run." and return immediately.
4. If `true` is returned, proceed with the polling logic.
5. **CRITICAL:** Wrap the polling logic in a `try-finally` block. In the `finally` block, call `distributedLockService.releaseLock("outbox_poller")` to ensure the lock is always released, even if an exception occurs.

### 3. Integration Testing (Testcontainers)
1. Create `DistributedSchedulingIT` in `backend/src/test/java/com/vantage/core/db/`.
2. Test 1 (Acquire/Release): Acquire the lock, assert `true`. Try to acquire it again, assert `false`. Release the lock. Try to acquire it again, assert `true`.
3. Test 2 (Poller Isolation): Mock or simulate two concurrent `OutboxPoller` executions in separate threads.
4. Verify that only one thread successfully acquires the lock and executes the repository query, while the other thread skips execution.

### 4. Verification (Output Instructions)
- Provide the command to run the test: `./gradlew test --tests "*DistributedSchedulingIT"`.
- Document that this allows the backend to be safely scaled to N instances on Render without duplicate message processing.

## Target File Paths
- `backend/src/main/java/com/vantage/core/db/DistributedLockService.java`
- `backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java` (Modify)
- `backend/src/test/java/com/vantage/core/db/DistributedSchedulingIT.java`
