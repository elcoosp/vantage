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
  echo "You are an autonomous AI engineering agent. You must follow the Execution Skill and Protocol strictly. Output ONLY one bash script per message that patches files, runs tests, and commits."
  echo ""
  echo "=================================================="
  echo "=== SECTION 1: AI AGENT EXECUTION SKILL (MUST READ) ==="
  echo "=================================================="
  if [ -f "docs/03-meta/agent-execution-skill.md" ]; then
    cat "docs/03-meta/agent-execution-skill.md"
  else
    echo "WARNING: docs/03-meta/agent-execution-skill.md not found."
  fi
  echo ""
  echo "=================================================="
  echo "=== SECTION 2: AI AGENT PROTOCOL & STANDARDS (MUST READ) ==="
  echo "=================================================="
  if [ -f "docs/03-meta/agent-protocol.md" ]; then
    cat "docs/03-meta/agent-protocol.md"
  else
    echo "WARNING: docs/03-meta/agent-protocol.md not found."
  fi
  echo ""
  echo "=================================================="
  echo "=== SECTION 3: TASK MANIFEST ==="
  echo "=================================================="
  cat "$TASK_FILE"
  echo ""
  echo "=================================================="
  echo "=== SECTION 4: INJECTED CONTEXT & CONTRACT FILES ==="
  echo "=================================================="

  # Extract explicit context files (like YAML contracts and DB schema) from the task manifest
  awk '/## Context Files to Inject/,/## Acceptance Criteria/' "$TASK_FILE" | grep -E '^\- `' | sed -E 's/^- `(.*)`.*$/\1/' | while read -r file; do
    if [ -f "$file" ]; then
      echo ""
      echo "--- FILE: $file ---"
      cat "$file"
      echo "--- END FILE: $file ---"
    else
      echo ""
      echo "--- WARNING: FILE NOT FOUND: $file ---"
    fi
  done

  echo ""
  echo "=================================================="
  echo "=== SECTION 5: CURRENT SOURCE CODE STATE (DO NOT REWRITE, PATCH ONLY) ==="
  echo "=================================================="
  echo "Below is the current state of all tracked files in the backend and frontend directories. Use this context to apply surgical patches (Pattern B) instead of rewriting files from scratch."
  echo ""

  echo "Using git ls-files to list tracked files, then filtering strictly for source code."
  echo "Excluding lock files, binaries, and wrappers to avoid bloating the AI context."

  git ls-files backend frontend | grep -iE '\.(java|kt|kts|yml|yaml|properties|xml|sql|ts|tsx|js|jsx|css|html|json|md)$|Dockerfile|Makefile|gradlew$' | grep -viE 'package-lock.json|pnpm-lock.yaml|yarn.lock|gradle-wrapper.jar|gradle-wrapper.properties' | while read -r file; do
    echo ""
    echo "--- EXISTING FILE: $file ---"
    cat "$file"
    echo "--- END EXISTING FILE: $file ---"
  done

  echo ""
  echo "=================================================="
  echo "=== SECTION 6: TARGET FILE PATHS TO CREATE/MODIFY ==="
  echo "=================================================="
  awk '/## Target File Paths/,0' "$TASK_FILE"
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
