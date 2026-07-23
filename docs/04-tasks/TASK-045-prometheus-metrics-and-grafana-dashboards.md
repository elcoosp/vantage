# TASK-045: Implement Prometheus Metrics & Grafana Dashboards

## Product Context
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section: Observability Strategy)
- Read: `docs/00-product/02-features-and-business-rules.md` (Understand system health monitoring)

## Objective
Complete the observability triad (Logs, Metrics, Traces) by exposing Prometheus metrics from the Spring Boot backend. Instrument key business operations (orders created, payments failed, inventory conflicts) and JVM/HTTP infrastructure metrics. Configure a local Grafana dashboard to visualize these metrics, proving production-ready monitoring capabilities.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/metrics/`, `backend/build.gradle.kts` (if micrometer-prometheus is missing), `docker-compose.yml` (to add Prometheus and Grafana), and `grafana/` (for provisioning).
- DO NOT modify domain logic or frontend code.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/build.gradle.kts`
- `docker-compose.yml`

## Acceptance Criteria

### 1. Micrometer Configuration
1. Ensure `micrometer-registry-prometheus` is present in `backend/build.gradle.kts`.
2. Update `application.yml` to expose the prometheus endpoint: `management.endpoints.web.exposure.include=health,prometheus,metrics`.
3. `management.metrics.export.prometheus.enabled=true`

### 2. Custom Business Metrics
1. Create `MetricsConfig` or use `MeterRegistry` directly in services.
2. Register a `Counter` for `vantage_orders_created_total` (tagged by `tenant_id`).
3. Register a `Counter` for `vantage_payments_failed_total` (tagged by `reason`).
4. Register a `Gauge` for `vantage_outbox_pending_events` (reads the count of `status = 'PENDING'` from the `OutboxRepository`).
5. Register a `Timer` for `vantage_payment_gateway_duration` to measure mock gateway latency.

### 3. Local Infrastructure (Docker Compose)
1. Add `prometheus/prometheus.yml` configuration file to scrape the backend at `host.docker.internal:8080/actuator/prometheus` every 5 seconds.
2. Add a `prometheus` service to `docker-compose.yml` (image: `prom/prometheus`, port 9090).
3. Add a `grafana` service to `docker-compose.yml` (image: `grafana/grafana`, port 3000).
4. Configure Grafana provisioning (`grafana/provisioning/datasources/datasource.yml`) to automatically add Prometheus as a data source.

### 4. Grafana Dashboard
1. Create a basic Grafana dashboard JSON model (`grafana/dashboards/vantage-overview.json`).
2. Panels should include:
   - JVM Memory Usage (Heap)
   - HTTP Request Rate (by status code)
   - Orders Created (per minute)
   - Payment Failures (per minute, by reason)
   - Outbox Pending Events (Gauge)
3. Add the dashboard to Grafana provisioning (`grafana/provisioning/dashboards/dashboard.yml`) so it loads automatically.

### 5. Verification (Output Instructions)
- Provide commands to start the stack: `docker-compose up -d prometheus grafana` and `./gradlew bootRun`.
- Verify metrics are exposed at `http://localhost:8080/actuator/prometheus`.
- Access Grafana at `http://localhost:3000` (admin/admin) and view the auto-provisioned dashboard.

## Target File Paths
- `docker-compose.yml` (Modify to add Prometheus and Grafana)
- `prometheus/prometheus.yml`
- `grafana/provisioning/datasources/datasource.yml`
- `grafana/provisioning/dashboards/dashboard.yml`
- `grafana/dashboards/vantage-overview.json`
- `backend/src/main/java/com/vantage/core/metrics/CustomMetricsConfig.java`
