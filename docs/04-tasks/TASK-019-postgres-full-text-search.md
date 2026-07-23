# TASK-019: Implement PostgreSQL Full-Text Search (FTS)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Understand the need for fast, unified search to power the Cmd+K palette)

## Objective
Implement a lightning-fast, unified search endpoint using native PostgreSQL Full-Text Search (`tsvector`, `tsquery`, and `GIN` indexes). This will power the frontend `Cmd+K` command palette, allowing vendors to instantly search across Products and Orders without the overhead of external search engines like Elasticsearch.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/search/`, `backend/src/main/java/com/vantage/product/`, `backend/src/main/java/com/vantage/order/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml`
- `docs/02-contracts/03-database-schema.md` (Reference: `products` and `orders` tables)
- `backend/src/main/java/com/vantage/product/domain/Product.java`
- `backend/src/main/java/com/vantage/order/domain/Order.java`

## Acceptance Criteria

### 1. Database Migration (Flyway)
1. Create a new Flyway migration script (e.g., `V2__add_fts_indexes.sql`).
2. Add a `tsvector` generated column to the `products` table, combining `name` and `sku`.
3. Add a `tsvector` generated column to the `orders` table, combining `status` and casting `id` to text.
4. Create `GIN` indexes on both new `tsvector` columns to ensure sub-millisecond query performance.

### 2. Search Repository & Service
1. Create `SearchRepository` in `com.vantage.core.search.domain`.
2. Use Spring Data JPA `@Query` with a native SQL query to perform `tsquery` against the `products` and `orders` tables.
3. The query must return a unified `SearchResult` projection containing `entityType` (PRODUCT/ORDER), `id`, `title`, and `description`.
4. The query must be scoped to the current `tenant_id` (rely on the Hibernate `@Filter` or explicitly add `WHERE tenant_id = :tenantId` in the native query).
5. Create `SearchService` in `com.vantage.core.search.app` to orchestrate the repository call.

### 3. REST API
1. Create `SearchController` in `com.vantage.core.search.ui` exposing `GET /api/v1/search?q={query}`.
2. The endpoint must accept a `query` string parameter.
3. Sanitize the input to prevent SQL injection (use `plainto_tsquery`).
4. Return a list of `SearchResultResponse` DTOs.

### 4. Integration Testing (Testcontainers)
1. Create `FullTextSearchIT` in `backend/src/test/java/com/vantage/core/search/`.
2. Setup: Register a vendor, create products ("Coffee Mug", "Coffee Beans"), and place an order.
3. Test 1: Search for "Coffee". Verify both products are returned.
4. Test 2: Search for "Mug". Verify only "Coffee Mug" is returned.
5. Test 3: Search for an order status (e.g., "CREATED"). Verify the order is returned.
6. Test 4 (Tenancy): Register a second vendor, create a product ("Tea Mug"). Search for "Mug" as Vendor A. Verify only Vendor A's "Coffee Mug" is returned.

## Target File Paths
- `backend/src/main/resources/db/migration/V2__add_fts_indexes.sql`
- `backend/src/main/java/com/vantage/core/search/domain/SearchResult.java`
- `backend/src/main/java/com/vantage/core/search/domain/SearchRepository.java`
- `backend/src/main/java/com/vantage/core/search/app/SearchService.java`
- `backend/src/main/java/com/vantage/core/search/ui/SearchController.java`
- `backend/src/main/java/com/vantage/core/search/ui/dto/SearchResultResponse.java`
- `backend/src/test/java/com/vantage/core/search/FullTextSearchIT.java`
