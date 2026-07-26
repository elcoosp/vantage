#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-045"
BRANCH="agent-1/TASK-045"
BASE_BRANCH="main"
TASK_ID="TASK-045"

cd "$WT"

echo "=== Pushing branch ==="
git push origin "$BRANCH"

echo "=== Creating PR ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(metrics): implement prometheus metrics and grafana dashboards for observability" \
  --body "## Summary
Implementation of TASK-045: Prometheus Metrics & Grafana Dashboards. This completes the observability triad by exposing key business and infrastructure metrics.

## Changes
- **Backend Dependencies**: Added micrometer-registry-prometheus to build.gradle.kts.
- **Configuration**: Updated application.yml to expose the /actuator/prometheus endpoint and enabled prometheus metrics export.
- **Custom Metrics**:
  - vantage_orders_created_total (Counter, tagged by tenant_id) in OrderService.
  - vantage_payments_failed_total (Counter, tagged by reason) in PaymentInventoryConsumer.
  - vantage_payment_gateway_duration (Timer) in MockPaymentGatewayClient.
  - vantage_outbox_pending_events (Gauge, O(1) memory efficient via countByStatus) in CustomMetricsConfig.
- **Infrastructure**: Added prometheus and grafana services to docker-compose.yml with auto-provisioning for datasources and the vantage-overview dashboard.
- **Test Fixes**: Updated 26 integration tests to use spring.datasource.primary.* and spring.datasource.replica.* to align with the custom DataSourceConfig. Fixed missing X-Tenant-ID header in VendorRegistrationIT and added SimpleMeterRegistry to MockPaymentGatewayClientTest.

## Testing
- All backend code compiles successfully.
- Metrics are exposed at http://localhost:8080/actuator/prometheus.
- Grafana dashboard is auto-provisioned and accessible at http://localhost:3000 (admin/admin).

Closes #TASK-045"

echo "✅ PR created"
