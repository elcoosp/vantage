# Vantage: Architecture Decision Records (ADR Log)

| Field | Value |
|-------|-------|
| Project | Vantage |
| Document | Architecture Decision Records (Level 3) |
| Version | 1.0 |
| Date | 2026-07-23 |
| Status | Approved |

## Introduction
This document logs the significant architectural decisions made during the design of Vantage. It captures the context, alternatives considered, and rationale behind each choice. AI agents must respect these decisions as immutable constraints unless superseded by a future ADR.

---

## ADR-001: Adopt Modular Monolith over Microservices

**Status:** Accepted  
**Drivers:** ASR-01 (Data Isolation), ASR-03 (Distributed Transactions), ASR-05 (Observability)

### Context
Vantage requires strict boundaries between domains (Vendor, Product, Inventory, Order). Deploying as microservices introduces operational overhead (Kubernetes, service mesh, distributed tracing setup) disproportionate to a portfolio project. However, a traditional monolith risks "spaghetti code" where modules directly query each other's repositories.

### Decision
Implement a Modular Monolith using Spring Modulith.

### Alternatives Considered
- **Microservices:** Rejected due to hosting costs ($0 budget constraint) and infrastructure complexity. 
- **Standard Monolith:** Rejected because it lacks enforced module boundaries, leading to tight coupling.

### Consequences
- **Positive:** Simplified deployment (single Render web service), easier local debugging, native support for Spring Modulith verification tests.
- **Negative:** Modules share the same database; strict discipline is required to ensure modules only communicate via Application Events or public service interfaces, not direct JPA joins.

---

## ADR-002: Hibernate Filters for Multi-Tenancy

**Status:** Accepted  
**Drivers:** ASR-01 (Data Isolation)

### Context
Vantage is a B2B SaaS platform. Vendors must never see each other's data. We needed a tenancy strategy that works within the $0 hosting budget (Neon.tech free tier).

### Decision
Use a shared database, shared schema model with Hibernate `@FilterDef` and `@Filter` annotations applied to a `BaseEntity`. The `tenant_id` is extracted from the JWT claim and stored in a `ThreadLocal` context by a servlet filter.

### Alternatives Considered
- **Schema-per-tenant:** Rejected. Flyway migrations would need to run dynamically for every new vendor signup, complicating the CI/CD pipeline.
- **Database-per-tenant:** Rejected. Neon free tier limits the number of databases; connection pool management becomes overly complex.

### Consequences
- **Positive:** Zero additional database costs. Onboarding a new vendor is simply inserting a row. 
- **Negative:** Risk of data leakage if a developer forgets to add the filter to a new entity. This is mitigated by enforcing all entities extend a `BaseEntity` and writing integration tests that explicitly verify cross-tenant access throws a `404 Not Found`.

---

## ADR-003: Transactional Outbox Pattern for Event Publishing

**Status:** Accepted  
**Drivers:** ASR-03 (Distributed Transactional Consistency)

### Context
When an order is placed, the system must save the `Order` to PostgreSQL and publish an `OrderCreated` event to RabbitMQ. If the system saves the order but RabbitMQ is down, the order is lost. If it publishes to RabbitMQ but the DB transaction fails, the inventory service will decrement stock for an order that doesn't exist (the dual-write problem).

### Decision
Implement the Transactional Outbox pattern. The `OrderService` writes the `Order` and an `OutboxEvent` record in the exact same database transaction. A separate scheduled background task polls the `outbox_events` table, publishes unpublished events to RabbitMQ, and marks them as `PUBLISHED` upon receiving a broker ACK.

### Alternatives Considered
- **2-Phase Commit (2PC):** Rejected. XA transactions are slow, brittle, and not supported well by modern message brokers.
- **Change Data Capture (CDC) via Debezium:** Rejected. Operating a Kafka Connect cluster exceeds the $0 budget and infrastructure complexity constraints.

### Consequences
- **Positive:** Guarantees at-least-once delivery. No distributed transaction coordinator required.
- **Negative:** Introduces a polling mechanism (every 2 seconds), adding a slight delay between order placement and inventory reservation. Requires consumers to be idempotent (handled via `processed_events` table).

---

## ADR-004: Saga Orchestrator over Event Choreography

**Status:** Accepted  
**Drivers:** ASR-04 (Fault Tolerance and Compensating Transactions)

### Context
The order flow spans multiple modules: `Order` -> `Inventory` -> `Payment`. If payment fails, inventory must be restored. 

### Decision
Implement an Orchestrated Saga. The `order` module maintains the state machine (`CREATED`, `VALIDATED`, `PAID`, `CANCELLED`). It listens for `InventoryReserved` and `PaymentFailed` events and explicitly issues `ReleaseInventory` commands.

### Alternatives Considered
- **Event Choreography:** `Inventory` listens to `OrderCreated`, `Payment` listens to `InventoryReserved`, `Inventory` listens to `PaymentFailed`. Rejected because the business flow is hidden across multiple modules, making it extremely difficult to trace and debug, especially for a recruiter trying to understand the system.

### Consequences
- **Positive:** The `order` module acts as the single source of truth for order state. Compensating logic is centralized and easy to visualize in OpenTelemetry traces.
- **Negative:** The `order` module becomes more coupled to the saga logic. This is an acceptable trade-off for a platform of this scale.

---

## ADR-005: JPA Optimistic Locking for Inventory

**Status:** Accepted  
**Drivers:** ASR-02 (Flash-Sale Concurrency)

### Context
During a flash sale, multiple users may attempt to purchase the last item simultaneously. The system must prevent overselling without causing database lock contention that would crash the application.

### Decision
Use JPA `@Version` (Long) on the `Inventory` entity. If two threads read version 1 and attempt to write, the first succeeds (version becomes 2), and the second throws an `ObjectOptimisticLockingFailureException`.

### Alternatives Considered
- **Pessimistic Locking (`SELECT ... FOR UPDATE`):** Rejected. Locking the row until the transaction completes (which includes a slow external HTTP call to the payment gateway) would cause thread pool exhaustion and severe latency during flash sales.

### Consequences
- **Positive:** Zero database lock contention. High throughput.
- **Negative:** The second user's request fails. This must be handled gracefully by the React frontend (returning a `409 Conflict` and updating the UI to show the item is sold out).
