#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-034"
BRANCH="agent-1/TASK-034"
BASE_BRANCH="main"
REPO_ROOT="$(pwd)"

cd "$WT"

echo "=== Final quality check: compiling and running CacheInvalidationIT ==="
cd backend
./gradlew compileTestJava test --tests "*CacheInvalidationIT" --no-daemon
cd ..

echo "=== Pushing branch to origin ==="
git push origin "$BRANCH"

echo "=== Creating Pull Request ==="
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(core): implement event-driven cache invalidation with Caffeine" \
  --body "$(cat << EOF
## Summary
Implement event-driven cache invalidation for product catalog and AI forecasts using Caffeine.

## Changes
- Add \`CacheConfig\` with Caffeine caches:
  - \`productCache\`: max 500, 1 hour TTL
  - \`forecastCache\`: max 100, 10 minutes TTL
- Annotate \`ProductService.getProductById\` with \`@Cacheable\` and \`updateProduct\`/\\\`deleteProduct\\\` with \`@CacheEvict\`
- Add \`AnalyticsService.getForecast\` with \`@Cacheable\` and delegate from \`AnalyticsController\`
- Create \`OrderCreatedEvent\` in \`core.events\` (cross-module event)
- Publish \`OrderCreatedEvent\` from \`OrderService\` after order creation
- Implement \`ForecastCacheEvictionListener\` in \`analytics\` module to evict \`forecastCache\` on event
- Integration test \`CacheInvalidationIT\` validates caching and eviction

## Testing
- Added \`CacheInvalidationIT\` using Testcontainers (PostgreSQL + RabbitMQ)
- Verifies:
  - First forecast call populates cache
  - Second call returns cached result
  - New order triggers eviction
  - Subsequent call recomputes forecast

Closes TASK-034
EOF
)"

echo "✅ PR created successfully"
