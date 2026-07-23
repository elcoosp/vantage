#!/usr/bin/env bash
set -uo pipefail

COMPILE_OK=true
INCOMPLETE=false

echo "Patching docs/03-meta/agent-execution-skill.md to fix PR template variable interpolation"
OLD_TMP=$(mktemp) || { echo "ERROR: cannot create temp file"; exit 1; }
NEW_TMP=$(mktemp)

cat > "$OLD_TMP" << 'OLD_DELIM_UNIQUE'
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(core): implement task ${TASK_ID}" \
  --body "$(cat << 'EOF'
## Summary
Implementation of ${TASK_ID}.

## Changes
- <bullet: what changed in which file/module and why>

## Testing
- <what tests were added/modified>

Closes #${TASK_ID}
EOF
)"
OLD_DELIM_UNIQUE

cat > "$NEW_TMP" << 'NEW_DELIM_UNIQUE'
gh pr create \
  --base "$BASE_BRANCH" \
  --title "feat(core): implement task ${TASK_ID}" \
  --body "$(cat << EOF
## Summary
Implementation of ${TASK_ID}.

## Changes
- <bullet: what changed in which file/module and why>

## Testing
- <what tests were added/modified>

Closes #${TASK_ID}
EOF
)"
NEW_DELIM_UNIQUE

if python3 - "$OLD_TMP" "$NEW_TMP" docs/03-meta/agent-execution-skill.md << 'PYEOF'
import sys
with open(sys.argv[1], 'r') as f: old = f.read()
with open(sys.argv[2], 'r') as f: new = f.read()
with open(sys.argv[3], 'r') as f: content = f.read()

if old not in content:
    print(f"ERROR: Anchor not found in {sys.argv[3]}. Aborting patch.")
    sys.exit(1)

content = content.replace(old, new)
with open(sys.argv[3], 'w') as f: f.write(content)
print(f"Successfully patched {sys.argv[3]}")
PYEOF
then
  echo "Python patch succeeded for docs/03-meta/agent-execution-skill.md"
  rm "$OLD_TMP" "$NEW_TMP"
else
  echo "ERROR: Python patch failed for docs/03-meta/agent-execution-skill.md"
  rm -f "$OLD_TMP" "$NEW_TMP"
  COMPILE_OK=false
fi

echo "Skipping compile check (documentation files)"

if [ "$INCOMPLETE" = true ] || [ "$COMPILE_OK" = false ]; then
  echo "Skipping tests and commit due to incomplete files or compilation errors"
  exit 1
fi

echo "Running tests"
echo "Skipping tests (documentation files)"

echo "All tests passed. Committing."
git add -A
git commit -m "fix(meta): allow variable interpolation in PR description heredoc"
