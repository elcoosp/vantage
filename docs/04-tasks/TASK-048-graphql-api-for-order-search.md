# TASK-048: Implement GraphQL API for Order Search

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 3 - The External Developer)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand API ecosystem)

## Objective
Implement a GraphQL API alongside the existing REST API to provide external developers with flexible, query-based access to the Vantage Order Search read model. This demonstrates mastery of modern API paradigms, allowing clients to request exactly the fields they need (e.g., just orderId and status) and reducing network overhead.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/graphql/`, `backend/src/main/java/com/vantage/order/query/`, `backend/build.gradle.kts` (to add Spring for GraphQL), and their corresponding test directories.
- DO NOT modify `application.yml`, frontend code, or REST controllers.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/03-database-schema.md` (Reference: `order_search_view`)
- `backend/src/main/java/com/vantage/order/query/domain/OrderSearchView.java`
- `backend/src/main/java/com/vantage/order/query/domain/OrderSearchViewRepository.java`
- `backend/build.gradle.kts`

## Acceptance Criteria

### 1. Dependency Setup
1. Add the `spring-boot-starter-graphql` dependency to `backend/build.gradle.kts`.
2. Add the `spring-graphql-test` dependency for testing.

### 2. GraphQL Schema Definition
1. Create `backend/src/main/resources/graphql/schema.graphqls`.
2. Define an `OrderSearchResult` type with fields: `orderId` (ID), `productName` (String), `status` (String), `quantity` (Int), `createdAt` (String).
3. Define a `Query` type with a method `orders(status: String, page: Int, size: Int): [OrderSearchResult]`.

### 3. GraphQL Controller Implementation
1. Create `OrderGraphQLController` in `com.vantage.core.graphql` (or `com.vantage.order.query.graphql`).
2. Annotate a method with `@QueryMapping("orders")`.
3. The method must accept `status`, `page`, and `size` parameters.
4. It must query the `OrderSearchViewRepository` (the CQRS read model from Task-021).
5. **CRITICAL:** It must enforce multi-tenancy by reading the `tenantId` from the `TenantContext` (populated by the security filter for the GraphQL endpoint `//graphql`).

### 4. Security Configuration
1. Ensure the `/graphql` endpoint is permitted in `SecurityConfig` for authenticated users (either JWT or API Key).
2. Ensure the GraphQL endpoint is subject to the `TenantFilterActivator` so the Hibernate filter applies if any direct JPA queries are used (though we are using the CQRS repository, it's good practice).

### 5. Integration Testing (Testcontainers)
1. Create `OrderGraphQLIT` in `backend/src/test/java/com/vantage/core/graphql/`.
2. Setup: Register a vendor, create a product, place an order, wait for CQRS projection.
3. Send a GraphQL query to `/graphql` requesting only `orderId` and `status`.
   ```graphql
   query {
     orders(status: "CREATED") {
       orderId
       status
     }
   }
   ```
4. Verify the response contains only the requested fields and the correct data.

## Target File Paths
- `backend/build.gradle.kts` (Modify to add GraphQL)
- `backend/src/main/resources/graphql/schema.graphqls`
- `backend/src/main/java/com/vantage/order/query/graphql/OrderGraphQLController.java`
- `backend/src/test/java/com/vantage/core/graphql/OrderGraphQLIT.java`
