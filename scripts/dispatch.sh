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
PROMPT_FILE=".dispatch_prompt_${TASK_ID}.txt"

echo "# AI AGENT TASK ASSIGNMENT" > "$PROMPT_FILE"
echo "" >> "$PROMPT_FILE"
cat "$TASK_FILE" >> "$PROMPT_FILE"
echo "" >> "$PROMPT_FILE"
echo "--- INJECTED CONTEXT FILES ---" >> "$PROMPT_FILE"

awk '/## Context Files to Inject/,/## Acceptance Criteria/' "$TASK_FILE" | grep -E '^\- \`' | sed -E 's/^- `(.*)`.*$/\1/' | while read -r file; do
  if [ -f "$file" ]; then
    echo "" >> "$PROMPT_FILE"
    echo "=== FILE: $file ===" >> "$PROMPT_FILE"
    cat "$file" >> "$PROMPT_FILE"
    echo "=== END FILE ===" >> "$PROMPT_FILE"
  else
    echo "" >> "$PROMPT_FILE"
    echo "=== WARNING: FILE NOT FOUND: $file ===" >> "$PROMPT_FILE"
  fi
done

echo "Prompt generated at $PROMPT_FILE"

if command -v pbcopy &> /dev/null; then
  cat "$PROMPT_FILE" | pbcopy
  echo "Copied to clipboard (macOS)."
elif command -v xclip &> /dev/null; then
  cat "$PROMPT_FILE" | xclip -selection clipboard
  echo "Copied to clipboard (Linux)."
else
  echo "Clipboard tool not found. Please open $PROMPT_FILE and copy manually."
fi
