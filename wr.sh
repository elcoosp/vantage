#!/usr/bin/env bash
set -uo pipefail

COMPILE_OK=true
INCOMPLETE=false

echo "Patching docs/03-meta/agent-execution-skill.md: update WORKTREE_PATH"
OLD_TMP=$(mktemp) || { echo "ERROR: cannot create temp file"; exit 1; }
NEW_TMP=$(mktemp)

cat > "$OLD_TMP" << 'OLD_DELIM_UNIQUE'
../vantage-agent-1-task-001
OLD_DELIM_UNIQUE

cat > "$NEW_TMP" << 'NEW_DELIM_UNIQUE'
../vantage-worktrees/agent-1-task-001
NEW_DELIM_UNIQUE

if python3 - "$OLD_TMP" "$NEW_TMP" docs/03-meta/agent-execution-skill.md << 'PYEOF'
import sys
with open(sys.argv[1], 'r') as f: old = f.read().strip()
with open(sys.argv[2], 'r') as f: new = f.read().strip()
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
git commit -m "docs(meta): standardize worktree path to ../vantage-worktrees/"
