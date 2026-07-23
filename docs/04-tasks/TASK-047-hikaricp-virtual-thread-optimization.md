# TASK-047: Implement HikariCP Virtual Thread Optimization & Connection Pool Tuning

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 2. Backend Scaffolding)
- Read: `docs/03-meta/agent-protocol.md` (Section 2. Technology Stack Constraints)

## Objective
Properly configure the HikariCP database connection pool for Java 21 Virtual Threads. By default, JDBC operations can pin carrier threads when using Virtual Threads. This task involves configuring HikariCP and the PostgreSQL driver to use the virtual-thread-per-task executor correctly, preventing pinning and maximizing throughput during flash-sale scenarios. This demonstrates deep JVM and database internals knowledge.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/resources/application.yml`, `backend/src/main/java/com/vantage/core/config/`, and `backend/build.gradle.kts` (if a specific driver version is needed).
- DO NOT modify domain logic, entities, or frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/vantage/core/config/JpaAuditingConfig.java`

## Acceptance Criteria

### 1. HikariCP Configuration
1. Update `application.yml` to explicitly tune HikariCP:
   - `maximum-pool-size: 20` (Optimal for PostgreSQL and typical container limits).
   - `minimum-idle: 10`.
   - `connection-timeout: 2000` (Fail fast if pool is exhausted).
   - `idle-timeout: 600000`.
   - `max-lifetime: 1800000`.
2. **CRITICAL (Virtual Thread Pinning):** Ensure the PostgreSQL JDBC driver is updated to a version that supports Virtual Threads without pinning (e.g., 42.7.2+). The driver natively avoids pinning on socket reads/writes in Java 21.

### 2. Async DataSource Configuration (Optional but Recommended)
1. If using `AbstractRoutingDataSource` (from Task-039), ensure the `ReplicaRoutingDataSource` is also wrapped or configured to work efficiently with virtual threads.
2. In `DataSourceConfig`, set `setMaximumPoolSize` and `setMinimumIdle` programmatically to ensure overrides take precedence over default Spring Boot properties.

### 3. Verification Test
1. Create `VirtualThreadPoolingIT` in `backend/src/test/java/com/vantage/core/config/`.
2. Use a loop to spawn 1,000 virtual threads (`Thread.startVirtualThread`).
3. Inside each thread, perform a simple JPA query (e.g., `productRepository.count()`).
4. Assert that all 1,000 queries complete successfully.
5. Log the carrier thread count (`ManagementFactory.getThreadMXBean()`) to demonstrate that thousands of virtual threads are multiplexed onto a small number of platform (carrier) threads without starvation or pinning.

### 4. Verification (Output Instructions)
- Provide the command to run the test: `./gradlew test --tests "*VirtualThreadPoolingIT"`.
- Document that the test proves database operations do not pin carrier threads.

## Target File Paths
- `backend/src/main/resources/application.yml` (Modify to add Hikari settings)
- `backend/src/main/java/com/vantage/core/config/DataSourceConfig.java` (Modify or create to ensure programmatic pool sizing)
- `backend/src/test/java/com/vantage/core/config/VirtualThreadPoolingIT.java`
