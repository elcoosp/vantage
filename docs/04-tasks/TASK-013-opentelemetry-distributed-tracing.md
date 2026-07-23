# TASK-013: Implement OpenTelemetry & Distributed Tracing

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Observability Strategy)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: 6.2 Observability Strategy)

## Objective
Instrument the Spring Boot backend with OpenTelemetry to enable end-to-end distributed tracing. The system must propagate trace IDs from incoming HTTP requests through JPA database queries, the Transactional Outbox poller, RabbitMQ publishes, and asynchronous consumers. This enables a single order to be visualized as a 12-span waterfall in Grafana Tempo.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/observability/`, `backend/src/main/java/com/vantage/core/messaging/`, and `backend/src/main/resources/application.yml`.
- DO NOT modify domain logic or business rules in other modules, except to add trace annotations if necessary.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/vantage/core/messaging/config/RabbitMQConfig.java`
- `backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java`

## Acceptance Criteria

### 1. Dependencies & Configuration
1. Add Micrometer Tracing dependencies (`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`) to `build.gradle.kts` (if not already present from Task 001).
2. Update `application.yml` to configure the OTLP exporter endpoint (e.g., `http://localhost:4317` for local Grafana Agent, or Grafana Cloud URL).
3. Set `management.tracing.sampling.probability=1.0` (100% sampling for dev/demo).
4. Ensure `spring.application.name=vantage-backend` is set so it appears as the service name in traces.

### 2. HTTP & JPA Instrumentation
1. Verify that incoming HTTP requests are automatically instrumented by Spring Boot Actuator.
2. Verify that JPA queries are automatically instrumented, showing spans for `SELECT` and `INSERT` statements.
3. Create a custom `@Observed` annotation or use `@WithSpan` on the `OrderService.createOrder` method to explicitly name the span "order.create".

### 3. RabbitMQ Trace Propagation
1. **Publisher (OutboxPoller):** Before publishing the message to RabbitMQ, inject the `traceparent` and `tracestate` headers into the message properties. Use the OpenTelemetry API (`Tracer`, `Span`) to create a producer span.
2. **Consumer (InventoryOrderConsumer, PaymentSagaConsumer):** Extract the `traceparent` header from the incoming RabbitMQ message. Use it to restore the trace context and create a consumer span that links to the producer span.

### 4. Verification (Output Instructions)
- Provide instructions on how to run the backend with a local Grafana Agent (or OTel Collector) to export traces to Grafana Tempo.
- Describe the expected trace structure: HTTP POST -> OrderService -> JPA Save Order -> Outbox Poller -> RabbitMQ Publish -> Inventory Consumer -> JPA Update Inventory.

## Target File Paths
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/vantage/core/observability/TraceConfig.java` (If needed for custom bean configuration)
- `backend/src/main/java/com/vantage/core/messaging/app/OutboxPoller.java` (Modify to inject trace headers)
- `backend/src/main/java/com/vantage/inventory/messaging/InventoryOrderConsumer.java` (Modify to extract trace headers)
