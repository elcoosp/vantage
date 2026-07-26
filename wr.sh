#!/usr/bin/env bash
set -euo pipefail
WT="../vantage-worktrees/agent-1-task-014"
BRANCH="agent-1/TASK-014"
BASE_BRANCH="main"
TASK_ID="TASK-014"

cd "$WT"

echo "=== Final quality gate ==="
cd frontend
pnpm run build 2>&1
cd ..

echo "=== Pushing branch ==="
git push -u origin "$BRANCH" --force

echo "=== Creating PR ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(analytics): implement AI forecast visualization dashboard" \
  --body "$(cat << 'EOF'
## Summary
Implementation of TASK-014: AI Demand Forecasting frontend dashboard.

## Changes
- Added `recharts` and `date-fns` dependencies to `frontend/package.json`.
- Created `useForecast` custom hook using `@tanstack/react-query` to fetch forecast data from `/api/v1/analytics/forecast/{productId}`.
- Created `ForecastChart` component using Recharts `ComposedChart` to display predicted quantity (dashed line) and confidence interval (shaded area).
- Created `ForecastDashboard` component with a product selector, loading state, and error handling.
- Updated `App.tsx` to include the `/forecast` route.

## Testing
- Verified TypeScript compilation and Biome linting pass without errors.
- Verified Vite production build succeeds.
- UI correctly handles loading, error, and empty states.
EOF
)"

echo "✅ PR created"
