#!/usr/bin/env bash
set -euo pipefail
WT="../vantage-worktrees/agent-1-task-021"
BRANCH="agent-1/TASK-021"
BASE_BRANCH="main"
TASK_ID="TASK-021"
cd "$WT"

echo "=== Pushing branch ==="
git push origin "$BRANCH"

echo "=== Creating PR ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(order): implement CQRS read model for order search" \
  --body "$(cat << 'EOF'
## Summary
Implementation of TASK-021: CQRS Read Model for Order Search.

## Changes
- **Database**: Added `V3__create_order_search_view.sql` with optimized indexes for tenant and status lookups.
- **Read Model**: Created `OrderSearchView` entity and `OrderSearchViewRepository` for denormalized order queries.
- **Event Projection**: Implemented `OrderSearchProjector` to asynchronously update the read model from `OrderCreatedEvent`, `PaymentSucceededEvent`, and `PaymentFailedEvent`.
- **Query API**: Added `OrderQueryController` exposing `GET /api/v1/orders/search` with pagination and optional status filtering.
- **Modularity Fix**: Updated `OrderRequest` and `OrderCreatedPayload` to include `productName`, preventing `OrderService` from violating Spring Modulith boundaries by querying the `ProductRepository`.
- **Concurrency Fix**: Refactored `PaymentSagaConsumer` to use a dedicated queue (`vantage.order.payment.saga.events`) and state-based idempotency, eliminating race conditions with `WebhookDispatchConsumer`.

## Testing
- Added comprehensive `OrderSearchCqrsIT` validating projection creation and status updates via Testcontainers.
- Updated existing integration tests (`OrderOutboxIT`, `PaymentSagaCompensationIT`, `InventoryConsumerIT`) to accommodate the new `OrderRequest` signature.

Closes #TASK-021
EOF
)"

echo "✅ PR created successfully"
