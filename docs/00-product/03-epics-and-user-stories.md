# Vantage: Epics and User Stories

This document breaks down the product vision into actionable epics and user stories. AI tasks will be derived directly from these stories.

## Epic 1: Multi-Tenant Onboarding and Isolation
- **Story 1.1: Vendor Self-Registration** -> Derives `TASK-101`
  - AC1: Registration creates a Vendor entity and assigns a unique `tenant_id`.
  - AC2: JWT authentication is issued containing the `tenant_id` claim.
- **Story 1.2: Tenant Isolation Filter** -> Derives `TASK-102`
  - AC1: Hibernate `@Filter` is applied to all tenant entities using a `ThreadLocal` context.
  - AC2: Admin role bypasses the filter.

## Epic 2: Catalog and Inventory Management
- **Story 2.1: Optimistic Stock Updates** -> Derives `TASK-201`
  - AC1: `Inventory` entity uses `@Version`.
  - AC2: Concurrent updates return `409 Conflict`.
  - AC3: React 19 frontend uses `useOptimistic` and rolls back on 409.
- **Story 2.2: Bulk Product Upload** -> Derives `TASK-202`
  - AC1: CSV parsing processes rows using Java 21 Virtual Threads.

## Epic 3: The Distributed Order Engine (Saga)
- **Story 3.1: Transactional Order Creation** -> Derives `TASK-301 (Outbox)`
  - AC1: `Order` and `OutboxEvent` saved in the same transaction.
  - AC2: Poller publishes events to RabbitMQ with publisher confirms.
- **Story 3.2: Saga Compensation** -> Derives `TASK-302 (Chaos Monkey)`
  - AC1: `InventoryConsumer` decrements stock on `OrderCreated`.
  - AC2: `PaymentConsumer` calls mock gateway, emits `PaymentSuccess` or `PaymentFailed`.
  - AC3: On `PaymentFailed`, orchestrator publishes `ReleaseInventory`, stock is restored, Order status = `CANCELLED`.
- **Story 3.3: Resilient Payment Processing** -> Derives `TASK-303`
  - AC1: Payment call wrapped in Resilience4j `@Retry` and `@CircuitBreaker`.

## Epic 4: Developer Experience and Integrations
- **Story 4.1: Idempotent Payment API** -> Derives `TASK-401`
  - AC1: `Idempotency-Key` header required.
  - AC2: Duplicate keys return cached response within 24h TTL.
- **Story 4.2: HMAC-Signed Webhooks** -> Derives `TASK-402`
  - AC1: Webhooks signed with `X-Vantage-Signature`.
  - AC2: Exponential backoff retry; dead-letter queue after 5 attempts.

## Epic 5: AI Forecasting and Analytics
- **Story 5.1: Pure-Java Sales Forecasting** -> Derives `TASK-501`
  - AC1: Endpoint queries 30 days of order items.
  - AC2: Holt-Winters algorithm calculates forecast.
  - AC3: API returns 7-day array with confidence intervals.
- **Story 5.2: Forecast Visualization** -> Derives `TASK-502`
  - AC1: React frontend uses Recharts for solid (historical) and dashed (forecast) lines.
  - AC2: Shaded `<Area>` represents confidence interval.

## Epic 6: Real-Time Operations
- **Story 6.1: Live Ops Shipping Map** -> Derives `TASK-601`
  - AC1: Order `SHIPPED` triggers WebSocket message to `/topic/ops-map`.
  - AC2: Nominatim geocoding resolves city to lat/long.
  - AC3: React Leaflet.js drops pulsing pin in real-time.
- **Story 6.2: Power User Command Palette** -> Derives `TASK-602`
  - AC1: React frontend implements `cmdk`.
  - AC2: Commands execute React 19 Actions without page reload.
