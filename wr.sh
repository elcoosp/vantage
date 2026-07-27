#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-044"
cd "$WT"

echo "=== Resolving merge conflicts ==="

# Check if we are in a merge state
if ! git status --porcelain | grep -q "^UU"; then
    echo "No merge conflicts detected. Exiting."
    exit 0
fi

# For package.json: take our version (HEAD)
git checkout --ours -- frontend/package.json
git add frontend/package.json

# For package-lock.json: we modified it, origin/main deleted it. Keep ours.
git checkout --ours -- frontend/package-lock.json
git add frontend/package-lock.json

# Verify all conflicts are resolved
if git diff --name-only --diff-filter=U | grep -q .; then
    echo "There are still unresolved conflicts:"
    git diff --name-only --diff-filter=U
    exit 1
fi

echo "All conflicts resolved. Committing merge."
git commit -m "merge: resolve conflicts with origin/main by keeping HEAD (openapi generation) changes"

echo "=== Merge complete. Pushing to origin ==="
git push origin HEAD

echo "✅ Merge conflict resolution done."
