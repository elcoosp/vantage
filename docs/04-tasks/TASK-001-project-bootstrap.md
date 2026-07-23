# TASK-001: Bootstrap Vantage Monorepo and Core Infrastructure

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Understand the SaaS multi-tenant context)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Understand Spring Modulith boundaries)

## Objective
Initialize the Vantage monorepo. Scaffold the Spring Boot 3.4 backend with Java 21, the React 19 frontend, and configure the core infrastructure (PostgreSQL, RabbitMQ, Flyway, Spring Modulith). Establish the base classes required for multi-tenancy and auditing.

## Execution Boundaries
- You may create files in the root directory (`/`), `backend/`, and `frontend/`.
- You may modify `build.gradle.kts`, `settings.gradle.kts`, `application.yml`, and `docker-compose.yml`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/03-database-schema.md` (Reference for BaseEntity and auditing columns)

## Acceptance Criteria

### 1. Root Repository Setup
1. Create a `docker-compose.yml` at the root provisioning:
   - `postgres:16-alpine` (User: `vantage`, DB: `vantage_dev`, Password: `vantage_pw`)
   - `rabbitmq:3.13-management` (Standard ports)

### 2. Backend Scaffolding (Spring Boot 3.4 + Java 21)
1. Create `backend/build.gradle.kts` using Kotlin DSL.
2. Configure the `java-toolchain` to use Java 21.
3. Enable Spring Boot Virtual Threads (`spring.threads.virtual.enabled=true`).
4. Include dependencies: Spring Web, Spring Security, Spring Data JPA, Spring Modulith, Spring AMQP, Flyway, PostgreSQL Driver, Lombok, Spring Boot Actuator, Resilience4j, Testcontainers, and Micrometer Tracing (OTel bridge).
5. Create `backend/src/main/resources/application.yml` configured to connect to the local Docker Compose services, enable Flyway, and enable Hibernate physical naming strategy.

### 3. Core Module and Base Classes
1. Create the base package structure: `com.vantage.core`, `com.vantage.vendor`, `com.vantage.product`, `com.vantage.inventory`, `com.vantage.order`, `com.vantage.payment`, `com.vantage.analytics`, `com.vantage.integration`.
2. Create `BaseEntity` in `com.vantage.core.domain`:
   - Fields: `id` (UUID, generated), `createdAt` (Instant, `@CreatedDate`), `updatedAt` (Instant, `@LastModifiedDate`).
   - Annotate with `@EntityListeners(AuditingEntityListener.class)`.
3. Create `BaseTenantEntity` extending `BaseEntity` in `com.vantage.core.domain`:
   - Field: `tenantId` (UUID).
   - Annotate with `@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))` and `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")`.
4. Create `TenantContext` in `com.vantage.core.tenant`:
   - A static class managing a `ThreadLocal<String>` to hold the current tenant ID.
   - Methods: `setTenantId(String)`, `getTenantId()`, `clear()`.
5. Enable JPA Auditing via `@EnableJpaAuditing` on a configuration class in `com.vantage.core.config`.

### 4. Frontend Scaffolding (React 19 + Vite)
1. Initialize a Vite project in the `frontend/` directory using the `react-ts` template.
2. Update `package.json` to use React 19 and React DOM 19.
3. Install dependencies: `@tanstack/react-query`, `axios`, `react-router-dom`, `tailwindcss`, `zustand`.
4. Configure `tailwind.config.js` and `src/index.css` with Tailwind directives.
5. Create a basic `src/App.tsx` that renders a header "Vantage Dashboard" to verify the setup.

### 5. Verification (Output Instructions)
- Provide the terminal commands to run the backend and frontend successfully.
- Do not write unit tests for this task; focus purely on infrastructure compilation and startup.

## Target File Paths
- `docker-compose.yml`
- `backend/build.gradle.kts`
- `backend/settings.gradle.kts`
- `backend/src/main/java/com/vantage/VantageApplication.java`
- `backend/src/main/java/com/vantage/core/config/JpaAuditingConfig.java`
- `backend/src/main/java/com/vantage/core/domain/BaseEntity.java`
- `backend/src/main/java/com/vantage/core/domain/BaseTenantEntity.java`
- `backend/src/main/java/com/vantage/core/tenant/TenantContext.java`
- `backend/src/main/resources/application.yml`
- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/tailwind.config.js`
- `frontend/src/App.tsx`
- `frontend/src/main.tsx`
- `frontend/src/index.css`
