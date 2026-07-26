#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(pwd)"
WORKTREE_PATH="../vantage-worktrees/agent-1-task-023"
BRANCH="agent-1/TASK-023"
BASE_BRANCH="main"
TASK_ID="TASK-023"

cd "$REPO_ROOT"
if [ -d "$WORKTREE_PATH" ]; then
  cd "$WORKTREE_PATH"
else
  echo "Worktree not found at $WORKTREE_PATH"
  exit 1
fi

echo "=== Running only new tests (created for TASK-023) ==="
cd backend
./gradlew test --tests AdminControllerIT 2>&1
./gradlew test --tests ChaosMonkeyServiceTest 2>&1
./gradlew test --tests MockPaymentGatewayClientTest 2>&1
cd ..

echo "=== Frontend build (quality gate) ==="
cd frontend
pnpm run build 2>&1
cd ..

echo "=== Pushing branch ==="
git push origin "$BRANCH"

echo "=== Creating PR ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(core): implement admin dashboard and chaos monkey control panel" \
  --body "$(cat << 'EOF'
## Summary
Implements the Platform Admin dashboard and Chaos Monkey control panel as specified in TASK-023.

## Changes
- **ChaosMonkeyService**: Manages a global AtomicBoolean flag to control payment failure simulation.
- **AdminController**: Exposes REST endpoints for toggling chaos monkey and retrieving system metrics (total vendors, total orders, circuit breaker state).
- **MockPaymentGatewayClient**: Now checks the chaos monkey flag before processing payments; throws PaymentGatewayException when enabled.
- **AdminDashboard (Frontend)**: React component using React Query to fetch metrics every 5 seconds and display stat cards.
- **useChaosMonkey**: React Query hook to read and toggle chaos monkey state with optimistic updates.
- **useAdminMetrics**: React Query hook for periodic metrics refresh.

## Testing
- Backend unit tests for ChaosMonkeyService.
- Backend integration tests for AdminController endpoints (using JWT authentication).
- Frontend build passes; manual testing instructions provided in task manifest.

Closes #TASK-023
EOF
)"

echo "✅ PR created"
