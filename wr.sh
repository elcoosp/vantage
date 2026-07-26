#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-039"
BRANCH="agent-1/TASK-039"
BASE_BRANCH="main"
TASK_ID="TASK-039"

cd "$WT"

echo "=== Quality gate: compile and run ReadReplicaRoutingIT only ==="
cd backend
./gradlew compileJava compileTestJava test --tests "ReadReplicaRoutingIT" 2>&1
cd ..

echo "=== Pushing branch ==="
git push origin "$BRANCH"

echo "=== Creating PR ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(db): implement read replica routing for scalability" \
  --body "## Summary
Implement database read/write splitting using Spring's AbstractRoutingDataSource. This enables offloading read-heavy operations (e.g., product searches, analytics forecasts) to a PostgreSQL read replica while directing writes (orders, inventory) to the primary node.

## Changes
- Added DatabaseType enum, DatabaseContextHolder (ThreadLocal), and ReplicaRoutingDataSource extending AbstractRoutingDataSource.
- Created DataSourceConfig to configure primary and replica DataSources and the routing DataSource as the primary bean.
- Implemented ReplicaRoutingInterceptor (AspectJ) that intercepts @Transactional methods:
  - Sets DatabaseType.REPLICA for readOnly = true transactions.
  - Sets DatabaseType.PRIMARY for write transactions.
  - Clears context after each invocation.
- Updated application.yml to support two datasource configurations (primary and replica).
- Added GET /api/v1/products/{id} endpoint to ProductController for read test.
- Integration test ReadReplicaRoutingIT using Testcontainers with two PostgreSQL containers (primary and replica), verifying routing via log capture.

## Testing
- Integration test spins up two PostgreSQL containers and a mocked RabbitMQ environment.
- Test 1 (Read): Calls GET /api/v1/products/{id} and asserts routing log shows REPLICA.
- Test 2 (Write): Calls POST /api/v1/orders and asserts routing log shows PRIMARY.

Closes #${TASK_ID}"

echo "✅ PR created"
