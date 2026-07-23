# TASK-003: Implement Product Catalog and Optimistic Inventory

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Inventory Concurrency)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 2.1: Optimistic Stock Updates)

## Objective
Implement the Product catalog and Inventory modules. Inventory updates must use JPA `@Version` for optimistic locking to handle flash-sale concurrency. The API must accept an `If-Match` header and return a `409 Conflict` if the version does not match.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/product/`, `backend/src/main/java/com/vantage/inventory/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ProductRequest`, `ProductResponse`, `InventoryUpdateRequest`, `InventoryResponse`, `If-Match` header)
- `docs/02-contracts/03-database-schema.md` (Reference: `products` and `inventory` tables)
- `backend/src/main/java/com/vantage/core/domain/BaseTenantEntity.java`

## Acceptance Criteria

### 1. Product Module Implementation
1. Create `Product` entity extending `BaseTenantEntity` matching the `products` schema.
2. Create `ProductRepository` extending `JpaRepository`.
3. Create `ProductService` and `ProductController` exposing `POST /api/v1/products`.
4. When a product is created, dispatch a `ProductCreatedEvent` via Spring ApplicationEvent publisher.

### 2. Inventory Module Implementation
1. Create `Inventory` entity extending `BaseTenantEntity` matching the `inventory` schema.
2. Include a `version` field of type `Long` annotated with `@Version`.
3. Create `InventoryRepository` extending `JpaRepository`.
4. Create an ApplicationEvent listener in the `inventory` module for `ProductCreatedEvent`. When received, create a new `Inventory` record for that product with an initial quantity of 0.

### 3. Optimistic Locking API
1. Create `InventoryController` exposing `PUT /api/v1/inventory/{productId}`.
2. The endpoint must accept an `If-Match` header representing the current `version`.
3. The request body contains the new `quantity`.
4. Before saving, manually verify the `If-Match` header against the entity's version. If they differ, throw an exception.
5. Alternatively, rely on JPA's automatic `@Version` increment. If `ObjectOptimisticLockingFailureException` is thrown during the save, catch it and throw a custom `InventoryConflictException`.
6. Create a `GlobalExceptionHandler` (or add to existing) in `com.vantage.core.exception` to catch `InventoryConflictException` and return a `409 Conflict` with an `ErrorResponse` payload.
7. The `InventoryResponse` must include the new `version` number for the client to use in future `If-Match` headers.

### 4. Integration Testing (Testcontainers)
1. Create `InventoryConcurrencyIT` in `backend/src/test/java/com/vantage/inventory/`.
2. Register a vendor, create a product, and initialize inventory to 10.
3. Fetch the current version (e.g., `0`).
4. Spawn two `Thread`s simultaneously attempting to update the inventory using the same `If-Match: 0` header.
5. Assert that one thread succeeds (`200 OK`) and the other fails (`409 Conflict`).
6. Verify the database quantity is updated exactly once and the version is incremented.

## Target File Paths
- `backend/src/main/java/com/vantage/product/domain/Product.java`
- `backend/src/main/java/com/vantage/product/domain/ProductRepository.java`
- `backend/src/main/java/com/vantage/product/app/ProductService.java`
- `backend/src/main/java/com/vantage/product/app/ProductCreatedEvent.java`
- `backend/src/main/java/com/vantage/product/ui/ProductController.java`
- `backend/src/main/java/com/vantage/product/ui/dto/ProductRequest.java`
- `backend/src/main/java/com/vantage/product/ui/dto/ProductResponse.java`
- `backend/src/main/java/com/vantage/inventory/domain/Inventory.java`
- `backend/src/main/java/com/vantage/inventory/domain/InventoryRepository.java`
- `backend/src/main/java/com/vantage/inventory/app/InventoryService.java`
- `backend/src/main/java/com/vantage/inventory/app/InventoryConflictException.java`
- `backend/src/main/java/com/vantage/inventory/app/ProductCreatedEventListener.java`
- `backend/src/main/java/com/vantage/inventory/ui/InventoryController.java`
- `backend/src/main/java/com/vantage/inventory/ui/dto/InventoryUpdateRequest.java`
- `backend/src/main/java/com/vantage/inventory/ui/dto/InventoryResponse.java`
- `backend/src/main/java/com/vantage/core/exception/GlobalExceptionHandler.java`
- `backend/src/test/java/com/vantage/inventory/InventoryConcurrencyIT.java`
