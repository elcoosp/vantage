# TASK-029: Implement Bulk Product Upload via CSV (Java 21 Virtual Threads)

## Product Context
- Read: `docs/00-product/03-epics-and-user-stories.md` (Story 2.2: Bulk Product Upload)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 2. Backend Scaffolding - Virtual Threads)

## Objective
Implement a CSV bulk upload endpoint for the Product catalog. To demonstrate mastery of Java 21 concurrency, the parsing and database insertion of 1,000+ rows must be processed using Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`), ensuring high throughput without blocking the main Tomcat request threads.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/product/` and its corresponding test directory.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/` or other modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml` (Reference: `ProductRequest`, `ProductResponse`)
- `docs/02-contracts/03-database-schema.md` (Reference: `products` table)
- `backend/src/main/java/com/vantage/product/domain/Product.java`
- `backend/src/main/java/com/vantage/product/domain/ProductRepository.java`

## Acceptance Criteria

### 1. CSV Parsing and Upload Endpoint
1. Create `BulkUploadController` in `com.vantage.product.ui` exposing `POST /api/v1/products/bulk-upload`.
2. The endpoint must accept `multipart/form-data` with a file field named `file`.
3. Parse the CSV file (columns: `name`, `price`, `sku`). Use a library like OpenCSV or Apache Commons CSV.

### 2. Virtual Thread Processing
1. Create `BulkUploadService` in `com.vantage.product.app`.
2. Read the CSV records into a list.
3. Use `try (var executor = Executors.newVirtualThreadPerTaskExecutor())` to process the records concurrently.
4. Submit each record (or batches of records) to the executor.
5. Inside the virtual thread task:
   - Validate the record (e.g., price > 0, name not empty).
   - Construct a `Product` entity with the current `tenantId`.
   - Save the product via `ProductRepository`.
6. Use a `CountDownLatch` or `Future.get()` to wait for all virtual threads to complete before returning the HTTP response.
7. Handle exceptions gracefully: if a record fails validation, collect the error and continue processing the rest. Return a summary response.

### 3. API Response
1. Return a `BulkUploadResponse` DTO containing:
   - `totalRecords` (int)
   - `successCount` (int)
   - `failureCount` (int)
   - `errors` (List of strings detailing which rows failed and why)
2. Return `200 OK` if the file is processed (even if some rows fail validation), or `400 Bad Request` if the file is not a valid CSV.

### 4. Integration Testing (Testcontainers)
1. Create `BulkUploadIT` in `backend/src/test/java/com/vantage/product/`.
2. Setup: Register a vendor and obtain a JWT.
3. Generate a mock CSV file with 1,000 valid product rows and 5 invalid rows (e.g., missing name, negative price).
4. Upload the file to `POST /api/v1/products/bulk-upload`.
5. Verify the response shows `totalRecords: 1005`, `successCount: 1000`, `failureCount: 5`.
6. Verify the database contains exactly 1,000 products for that tenant.
7. Verify the endpoint completes in a reasonable time (e.g., < 5 seconds), demonstrating the efficiency of virtual threads.

## Target File Paths
- `backend/src/main/java/com/vantage/product/ui/BulkUploadController.java`
- `backend/src/main/java/com/vantage/product/app/BulkUploadService.java`
- `backend/src/main/java/com/vantage/product/ui/dto/BulkUploadResponse.java`
- `backend/src/test/java/com/vantage/product/BulkUploadIT.java`
