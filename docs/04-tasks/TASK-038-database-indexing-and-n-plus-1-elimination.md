# TASK-038: Database Query Optimization & N+1 Elimination

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Engineering Maturity)
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Flash-Sale Concurrency)

## Objective
Audit and optimize all JPA queries to eliminate the N+1 problem and ensure proper indexing. Use Hibernate statistics and `EXPLAIN ANALYZE` to verify that queries are hitting indexes and not performing full table scans. This demonstrates senior-level database performance tuning skills.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/db/migration/`, and their corresponding test directories.
- DO NOT modify frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/03-database-schema.md`
- `backend/src/main/java/com/vantage/order/domain/OrderRepository.java`
- `backend/src/main/java/com/vantage/product/domain/ProductRepository.java`
- `backend/src/main/java/com/vantage/inventory/domain/InventoryRepository.java`

## Acceptance Criteria

### 1. N+1 Query Audit and Fix
1. Enable Hibernate statistics in `application.yml` for the `dev` profile (`spring.jpa.properties.hibernate.generate_statistics=true`).
2. Audit all `@OneToMany` and `@ManyToOne` relationships in `Order`, `Product`, and `Inventory`.
3. Replace lazy loading in loops with `@EntityGraph` or `JOIN FETCH` in repository queries.
4. Specifically, optimize `OrderQueryController` (from CQRS Task-021) and any endpoints returning lists of products or orders to execute in a single SQL query.

### 2. Index Optimization (Flyway)
1. Create a new Flyway migration `V4__optimize_indexes.sql`.
2. Add partial indexes where applicable (e.g., `CREATE INDEX idx_orders_status_pending ON orders (tenant_id, status) WHERE status = 'CREATED';`).
3. Add composite indexes for common query patterns (e.g., `CREATE INDEX idx_products_tenant_sku ON products (tenant_id, sku);`).
4. Use `CONCURRENTLY` for index creation to avoid locking the table in production.

### 3. Verification Test
1. Create `QueryOptimizationIT` in `backend/src/test/java/com/vantage/core/`.
2. Seed 100 products and 100 orders for a tenant.
3. Fetch all orders with their associated products.
4. Enable Hibernate statistics in the test.
5. Assert that only 1 query was executed (verify `org.hibernate.stat.QueryStatistics` for the specific query has `executionCount == 1`).

### 4. Verification (Output Instructions)
- Provide the command to run the test: `./gradlew test --tests "*QueryOptimizationIT"`.
- Document how to view the generated statistics in the logs.

## Target File Paths
- `backend/src/main/resources/db/migration/V4__optimize_indexes.sql`
- `backend/src/main/java/com/vantage/order/domain/OrderRepository.java` (Modify to add JOIN FETCH)
- `backend/src/main/java/com/vantage/product/domain/ProductRepository.java` (Modify to add @EntityGraph)
- `backend/src/test/java/com/vantage/core/QueryOptimizationIT.java`
