# Vantage: Features and Business Rules

This document defines the exact business logic for the core features. AI agents must reference this document to understand the behavioral requirements of the system.

## 1. Multi-Tenancy and Isolation
- **Rule:** A vendor must never see another vendor's data.
- **Implementation Constraint:** Data isolation must be achieved at the ORM level using Hibernate `@Filter` and a `ThreadLocal` tenant context. 
- **Admin Exception:** Platform Admins must bypass the tenant filter to view global system data.

## 2. Inventory Concurrency (Flash Sale)
- **Rule:** If two orders attempt to purchase the last available item simultaneously, one order succeeds, and the other fails gracefully without database-level pessimistic locking.
- **Implementation Constraint:** The `Inventory` entity must use `@Version` for optimistic locking. Concurrent updates resulting in an `ObjectOptimisticLockingFailureException` must be caught and returned to the client as a `409 Conflict`.

## 3. Distributed Order Engine (Saga + Outbox)
- **Rule:** An order consists of: Reserve Inventory -> Process Payment -> Ship Order.
- **Dual-Write Problem:** The system must never write to the database and publish a RabbitMQ message independently. 
- **Outbox Pattern:** The `OrderService` must write the `Order` entity and an `OutboxEvent` in the exact same database transaction. A scheduled poller reads unpublished events and publishes them to RabbitMQ with publisher confirms.
- **Compensating Transactions (Chaos Monkey):** If the `ProcessPayment` step fails (e.g., simulated payment gateway timeout), the Saga orchestrator must fire a `ReleaseInventory` command to restore the reserved stock and mark the order as `CANCELLED`.

## 4. Resilience and Fault Tolerance
- **Rule:** Transient failures in external services (mock payment gateway, geocoding) must not cascade and bring down the system.
- **Implementation Constraint:** External calls must be wrapped in Resilience4j `@Retry`, `@CircuitBreaker`, and `@RateLimiter`. If a circuit is open, the system must fail fast and trigger compensating transactions.

## 5. Developer Experience (APIs & Webhooks)
- **Idempotency Rule:** `POST /api/v1/payments` requires an `Idempotency-Key` header. If a duplicate key is sent within 24 hours, the system must return the original cached response without reprocessing the payment.
- **Webhook Rule:** Order lifecycle events must be delivered to registered vendor URLs via HTTP POST. Payloads must be signed using HMAC-SHA256. Delivery must use exponential backoff with a dead-letter queue after 5 failed attempts.

## 6. AI Demand Forecasting
- **Rule:** The system must predict 7 days of future demand for a product based on the last 30 days of historical order data.
- **Implementation Constraint:** The forecast must be calculated using a pure Java Exponential Smoothing algorithm (Holt-Winters). The API must return a JSON array of future dates, predicted quantities, and a confidence interval percentage.
