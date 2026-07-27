#!/usr/bin/env bash
set -euo pipefail

WT="../vantage-worktrees/agent-1-task-026"
cd "$WT"

echo "=== Fetching latest main ==="
git fetch origin main

echo "=== Merging origin/main into current branch ==="
# Attempt merge, but don't commit automatically
if git merge origin/main --no-commit --no-ff; then
    echo "✅ No conflicts. Committing merge."
    git commit -m "chore: merge main into agent-1/TASK-026"
else
    echo "⚠️  Merge conflicts detected."

    # List conflicted files
    CONFLICTED=$(git diff --name-only --diff-filter=U)
    echo "Conflicted files:"
    echo "$CONFLICTED"

    # Resolve each conflicted file
    for file in $CONFLICTED; do
        echo "Resolving $file"
        # Decide which version to keep
        if [[ "$file" =~ ^backend/src/main/java/com/vantage/core/chat/ ]] || \
           [[ "$file" =~ ^frontend/src/features/chat/ ]] || \
           [[ "$file" == "frontend/src/components/Layout.tsx" ]]; then
            echo "  Keeping our version (--ours) for $file"
            git checkout --ours "$file"
        else
            echo "  Accepting main version (--theirs) for $file"
            git checkout --theirs "$file"
        fi
        git add "$file"
    done

    echo "=== Committing merge resolution ==="
    git commit -m "chore: resolve merge conflicts with main (keep chat changes, accept others from main)"
fi

echo "=== Pushing branch ==="
git push origin agent-1/TASK-026

echo "✅ Merge and push complete."
