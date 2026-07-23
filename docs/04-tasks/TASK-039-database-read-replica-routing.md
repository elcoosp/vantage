# TASK-039: Implement Database Read Replica Routing (ReplicaAwareDataSource)

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand scalability requirements)
- Read: `docs/00-product/01-vision-and-personas.md` (Understand high-traffic read scenarios like flash sales)

## Objective
Implement database read/write splitting using Spring's `AbstractRoutingDataSource`. In a production scenario, a SaaS platform must offload read-heavy operations (like product searches and AI forecasting data aggregation) to a PostgreSQL read replica, while directing writes (orders, inventory updates) to the primary node. This demonstrates elite-level database scaling and Spring infrastructure knowledge.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/db/`, `backend/src/main/java/com/vantage/product/`, `backend/src/main/java/com/vantage/analytics/`, `backend/src/main/resources/application.yml`, and their corresponding test directories.
- DO NOT modify frontend code or domain entities.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/vantage/product/app/ProductService.java`
- `backend/src/main/java/com/vantage/analytics/app/AnalyticsService.java`

## Acceptance Criteria

### 1. Routing DataSource Infrastructure
1. Create `DatabaseType` enum in `com.vantage.core.db` with values `PRIMARY` and `REPLICA`.
2. Create `DatabaseContextHolder` in `com.vantage.core.db` managing a `ThreadLocal<DatabaseType>`.
3. Create `ReplicaRoutingDataSource` extending `AbstractRoutingDataSource`.
   - Override `determineCurrentLookupKey()` to return `DatabaseContextHolder.getDatabaseType()`.
   - Configure `setLenientFallback(false)` to fail fast if the target data source is missing.
4. Create `DataSourceConfig` in `com.vantage.core.db`:
   - Configure two `DataSource` beans: `primaryDataSource` and `replicaDataSource` reading from `application.yml` (`spring.datasource.primary.*` and `spring.datasource.replica.*`).
   - Configure the `ReplicaRoutingDataSource` as the primary `@Bean` DataSource, mapping `DatabaseType.PRIMARY` to `primaryDataSource` and `DatabaseType.REPLICA` to `replicaDataSource`.

### 2. Transactional Routing Configuration
1. Create `ReplicaRoutingInterceptor` in `com.vantage.core.db` implementing `MethodInterceptor` (or use an AOP aspect).
2. Intercept methods annotated with `@Transactional`.
3. If `@Transactional(readOnly = true)` is present, set `DatabaseContextHolder.setDatabaseType(DatabaseType.REPLICA)`.
4. Otherwise, set `DatabaseContextHolder.setDatabaseType(DatabaseType.PRIMARY)`.
5. **CRITICAL:** In the `finally` block, always call `DatabaseContextHolder.clear()` to prevent thread leakage (especially with Virtual Threads).

### 3. Application Configuration
1. Update `application.yml` to support two datasources:
   - `spring.datasource.primary.url` (e.g., the Neon.tech primary connection string)
   - `spring.datasource.replica.url` (e.g., the Neon.tech read replica connection string)
2. Configure the `DataSourceConfig` to read these properties.

### 4. Service Layer Annotation
1. Ensure `ProductService.getProductById` is annotated with `@Transactional(readOnly = true)`.
2. Ensure `AnalyticsService.getForecast` is annotated with `@Transactional(readOnly = true)`.
3. Ensure `OrderService.createOrder` and `InventoryService.updateInventory` are annotated with standard `@Transactional` (read/write).

### 5. Integration Testing (Testcontainers)
1. Create `ReadReplicaRoutingIT` in `backend/src/test/java/com/vantage/core/db/`.
2. Use Testcontainers to spin up two PostgreSQL containers (simulating Primary and Replica).
3. Mock the `ReplicaRoutingDataSource` or use a custom aspect to log which database was hit.
4. Test 1 (Read): Call `GET /api/v1/products/{id}`. Verify the routing interceptor sets the context to `REPLICA`.
5. Test 2 (Write): Call `POST /api/v1/orders`. Verify the routing interceptor sets the context to `PRIMARY`.

## Target File Paths
- `backend/src/main/java/com/vantage/core/db/DatabaseType.java`
- `backend/src/main/java/com/vantage/core/db/DatabaseContextHolder.java`
- `backend/src/main/java/com/vantage/core/db/ReplicaRoutingDataSource.java`
- `backend/src/main/java/com/vantage/core/db/DataSourceConfig.java`
- `backend/src/main/java/com/vantage/core/db/ReplicaRoutingInterceptor.java`
- `backend/src/main/resources/application.yml` (Modify to add replica config)
- `backend/src/main/java/com/vantage/product/app/ProductService.java` (Modify to enforce readOnly)
- `backend/src/main/java/com/vantage/analytics/app/AnalyticsService.java` (Modify to enforce readOnly)
- `backend/src/test/java/com/vantage/core/db/ReadReplicaRoutingIT.java`
