# Vantage: AI Agent Protocol & Engineering Standards

| Field | Value |
|-------|-------|
| Project | Vantage |
| Document | Agent Protocol & Standards |
| Version | 1.0 |
| Date | 2026-07-23 |
| Status | Approved |

## 1. Execution Boundaries
- You are an autonomous AI engineering agent. You must ONLY create or modify files explicitly listed in your task's "Execution Boundaries".
- Never modify `build.gradle.kts`, `application.yml`, or infrastructure code unless explicitly instructed.
- If you need a dependency that is not present, do not add it. Instead, output a comment requesting human review.

## 2. Technology Stack Constraints
- **Language:** Java 21. Use Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`), Records, and Pattern Matching where applicable. Do not use Java 8 idioms.
- **Framework:** Spring Boot 3.4. Use Spring Modulith boundaries.
- **Frontend:** React 19. Use Server Actions or `useOptimistic` where specified. Use TypeScript strict mode.
- **Database:** PostgreSQL 16 via Spring Data JPA. Hibernate `@Filter` for multi-tenancy.
- **Messaging:** RabbitMQ via Spring AMQP. Publisher confirms must be enabled.

## 3. Architectural Rules
- **Modularity:** Respect Spring Modulith boundaries. A module may NOT import another module's repository or internal entities. Cross-module communication must use Spring Application Events or public API interfaces.
- **Multi-Tenancy:** Every entity belonging to a tenant must extend `BaseTenantEntity` (which includes the `@Filter` definition). Never manually filter by `tenant_id` in repository queries; rely on the Hibernate filter.
- **Concurrency:** Use JPA `@Version` for optimistic locking on inventory entities. Catch `ObjectOptimisticLockingFailureException` and return `409 Conflict`.
- **Transactions:** Database writes and RabbitMQ publishes must NOT be in the same transaction. Use the Transactional Outbox pattern.

## 4. Coding Standards
- **No Comments:** Do not write Javadoc or inline comments unless the logic is extremely complex. The code must be self-documenting.
- **No Emojis:** Never use emojis in code, strings, or comments.
- **Naming:** Classes must be PascalCase. Methods and variables must be camelCase. Constants must be UPPER_SNAKE_CASE.
- **Exceptions:** Throw custom domain exceptions (e.g., `InventoryOutOfBoundsException`) and handle them in the `GlobalExceptionHandler`. Do not throw generic `RuntimeException`.

## 5. Testing Requirements
- All new features must include Testcontainers integration tests.
- Tests must run against actual PostgreSQL and RabbitMQ containers. Do not use H2 or in-memory mocks for integration tests.
- Test method names must use the `should_<expected_behavior>_when_< precondition>` format.

## 6. Output Format
- Output code in standard Markdown code blocks.
- If creating a new file, output the full absolute path as a comment on the first line (e.g., `// backend/src/main/java/com/vantage/inventory/InventoryService.java`).
- Do not output partial classes. If a file is being modified, output the entire file content.
