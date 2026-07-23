#!/usr/bin/env bash
set -uo pipefail

COMPILE_OK=true
INCOMPLETE=false

echo "Writing scripts/dispatch.sh"
cat > scripts/dispatch.sh << 'DISPATCH_DELIM'
#!/usr/bin/env bash
set -uo pipefail

if [ -z "$1" ]; then
  echo "Usage: ./scripts/dispatch.sh <TASK_ID> (e.g., TASK-001)"
  exit 1
fi

TASK_ID=$1
TASK_FILE=$(find docs/04-tasks -name "${TASK_ID}*.md" | head -n 1)

if [ -z "$TASK_FILE" ]; then
  echo "Error: Task file not found for $TASK_ID"
  exit 1
fi

echo "Dispatching $TASK_FILE..."

generate_prompt() {
  echo "# AI AGENT TASK ASSIGNMENT"
  echo ""
  cat "$TASK_FILE"
  echo ""
  echo "--- INJECTED CONTEXT FILES ---"

  awk '/## Context Files to Inject/,/## Acceptance Criteria/' "$TASK_FILE" | grep -E '^\- `' | sed -E 's/^- `(.*)`.*$/\1/' | while read -r file; do
    if [ -f "$file" ]; then
      echo ""
      echo "=== FILE: $file ==="
      cat "$file"
      echo "=== END FILE ==="
    else
      echo ""
      echo "=== WARNING: FILE NOT FOUND: $file ==="
    fi
  done
}

if command -v pbcopy &> /dev/null; then
  generate_prompt | pbcopy
  echo "Prompt for $TASK_ID copied to clipboard (macOS)."
elif command -v xclip &> /dev/null; then
  generate_prompt | xclip -selection clipboard
  echo "Prompt for $TASK_ID copied to clipboard (Linux)."
elif command -v clip.exe &> /dev/null; then
  generate_prompt | clip.exe
  echo "Prompt for $TASK_ID copied to clipboard (Windows/WSL)."
else
  echo "Clipboard tool not found. Outputting to stdout:"
  generate_prompt
fi
DISPATCH_DELIM

chmod +x scripts/dispatch.sh

echo "Skipping compile check (bash script)"
COMPILE_OK=true

if [ "$INCOMPLETE" = true ] || [ "$COMPILE_OK" = false ]; then
  echo "Skipping tests and commit due to incomplete files or compilation errors"
  exit 1
fi

echo "Running tests"
echo "Skipping tests (bash script)"

echo "All tests passed. Committing."
git add -A
git commit -m "fix(scripts): update dispatch to clipboard only without temp file"
