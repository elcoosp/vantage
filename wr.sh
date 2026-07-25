#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-020"
cd "$WT"

echo "=== Pushing branch to remote ==="
git push origin agent-1/TASK-020

echo "=== Creating Pull Request ==="
gh pr create \
  --base main \
  --title "feat(integration): implement API key authentication and management (TASK-020)" \
  --body "$(cat << EOF
## Summary
Implement a Stripe-style API key system for external developers. Vendors can now generate API keys via the dashboard, and external systems can authenticate using the \`X-API-Key\` header.

## Changes
- Added \`ApiKey\` entity in \`integration.domain\` with BCrypt hashed keys and tenant isolation.
- Added \`ApiKeyRepository\` with custom query for active keys by prefix.
- Added \`ApiKeyService\` for generating and revoking keys (returns plain key once).
- Added \`ApiKeyController\` exposing \`POST /api/v1/api-keys\`, \`GET /api/v1/api-keys\`, and \`DELETE /api/v1/api-keys/{id}\`.
- Extended \`TenantSecurityFilter\` to support \`X-API-Key\` header authentication; falls back to JWT if missing.
- Added Flyway migration \`V3__add_api_keys.sql\` to create the \`api_keys\` table.
- Added integration tests (Testcontainers) covering valid key, missing key, invalid key, and revoked key scenarios.

## Testing
- \`ApiKeyAuthenticationIT\` test class verifies:
  - Successful product creation with a valid API key (returns 200 OK).
  - 401 Unauthorized when no key is provided.
  - 401 Unauthorized when an invalid key is used.
  - 401 Unauthorized when a revoked key is used.

Closes #TASK-020
EOF
)"

echo "✅ PR created successfully"
