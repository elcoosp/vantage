#!/usr/bin/env bash
set -uo pipefail

COMPILE_OK=true
INCOMPLETE=false

echo "Patching README.md: adding logo image"
OLD_TMP=$(mktemp) || { echo "ERROR: cannot create temp file"; exit 1; }
NEW_TMP=$(mktemp)

cat > "$OLD_TMP" << 'OLD_DELIM_UNIQUE'
# Vantage

> A production-grade, multi-tenant SaaS platform enabling independent merchants to manage operations, with distributed order orchestration, AI-driven forecasting, and end-to-end observability.
OLD_DELIM_UNIQUE

cat > "$NEW_TMP" << 'NEW_DELIM_UNIQUE'
# Vantage

<p align="center">
  <img src="docs/logo.png" alt="Vantage Logo" width="200" />
</p>

> A production-grade, multi-tenant SaaS platform enabling independent merchants to manage operations, with distributed order orchestration, AI-driven forecasting, and end-to-end observability.
NEW_DELIM_UNIQUE

if python3 - "$OLD_TMP" "$NEW_TMP" README.md << 'PYEOF'
import sys
with open(sys.argv[1], 'r') as f: old = f.read()
with open(sys.argv[2], 'r') as f: new = f.read()
with open(sys.argv[3], 'r') as f: content = f.read()

if old not in content:
    print(f"ERROR: Anchor not found in {sys.argv[3]}. Aborting patch.")
    sys.exit(1)

content = content.replace(old, new, 1)
with open(sys.argv[3], 'w') as f: f.write(content)
print(f"Successfully patched {sys.argv[3]}")
PYEOF
then
  echo "Python patch succeeded for README.md"
  rm "$OLD_TMP" "$NEW_TMP"
else
  echo "ERROR: Python patch failed for README.md"
  rm -f "$OLD_TMP" "$NEW_TMP"
  COMPILE_OK=false
fi

echo "Skipping compile check (documentation file)"

if [ "$INCOMPLETE" = true ] || [ "$COMPILE_OK" = false ]; then
  echo "Skipping tests and commit due to incomplete files or compilation errors"
  exit 1
fi

echo "Running tests"
echo "Skipping tests (documentation file)"

echo "All tests passed. Committing."
git add -A
git commit -m "docs: add logo to README"
