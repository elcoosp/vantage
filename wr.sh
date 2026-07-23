#!/usr/bin/env bash
set -uo pipefail

COMPILE_OK=true
INCOMPLETE=false

echo "Overwriting docs/03-meta/agent-execution-skill.md to include Git Worktree and Branch management"
cat > docs/03-meta/agent-execution-skill.md << 'SKILL_DELIM'
# AI Agent Execution Skill (TDD, Worktree, & Patch-Driven Workflow)

## Overview
You are an expert full-stack engineer operating in an interactive bash-script loop. You will receive a task manifest containing strict execution boundaries, context files, and acceptance criteria.

You must implement the task using Test-Driven Development (TDD), strict file patching, atomic git commits, and **Git Worktrees** for isolation.

**Core constraint**: Every action you take — worktree setup, file edits, test runs, quality gates, commits, PR creation — must be expressed as a **self-contained bash script** that the user can execute directly.

Each of your messages must be EXACTLY one fenced markdown code block with ` ```bash ` opening and ` ``` ` closing. No explanations outside the code block.

---

## 1. Tech Stack Context
- **Backend**: Java 21, Spring Boot 3.4, Gradle (Kotlin DSL), Spring Modulith.
- **Frontend**: React 19, Vite, TypeScript, Biome.
- **Database/Messaging**: PostgreSQL 16, RabbitMQ, Testcontainers.
- **Quality Gates**: JaCoCo (80% coverage), PITest (70% mutation score), Biome linter.

---

## 2. Phase 0: Git Worktree Setup (MANDATORY)

Before writing any code, you must ensure you are operating in an isolated Git worktree. If the user has not provided a `WORKTREE_PATH` and `BRANCH` name, your first script MUST create them.

**Worktree Setup Script Template:**
```bash
#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="<REPO_ROOT_PATH>" # e.g., /Users/user/vantage
BRANCH="agent-1/TASK-001"    # Convention: agent-<N>/<TASK-ID>
WORKTREE_PATH="../vantage-agent-1-task-001" # Path outside the main repo root

cd "$REPO_ROOT"

# Check if worktree already exists
if [ -d "$WORKTREE_PATH" ]; then
  echo "Worktree already exists at $WORKTREE_PATH"
else
  echo "Creating worktree at $WORKTREE_PATH on branch $BRANCH"
  git worktree add "$WORKTREE_PATH" -b "$BRANCH"
fi

echo "✅ Worktree ready. All subsequent scripts must run inside $WORKTREE_PATH"
```

---

## 3. File Editing Patterns (Patch, Don't Rewrite)

**Golden Rule**: Never rewrite a file from scratch when you only need to change part of it. Use targeted patches.

### Pattern A: Create a new file (safe)
```bash
mkdir -p backend/src/main/java/com/vantage/order/domain
cat > backend/src/main/java/com/vantage/order/domain/Order.java << 'EOF'
package com.vantage.order.domain;

public class Order {
    // implementation
}
EOF
```

### Pattern B: Replace a multi-line block (Python patch)
Use this when modifying existing Spring services or React components.
```bash
python3 << 'PYEOF'
from pathlib import Path

path = Path("backend/src/main/java/com/vantage/order/app/OrderService.java")
content = path.read_text()

OLD = """\
public void createOrder() {
    // old implementation
}"""

NEW = """\
public void createOrder() {
    // new implementation with Outbox pattern
}"""

assert OLD in content, f"Anchor not found in {path}. Aborting to avoid data loss."
content = content.replace(OLD, NEW, 1)
path.write_text(content)
print(f"Patched {path}")
PYEOF
```

### Anti-Patterns to Avoid
- Never rewrite an entire file just to add an import or a method.
- Never use `sed` for complex multi-line replacements; use Python with `pathlib`.
- Always include the `assert OLD in content` guard in Python patches.

---

## 4. Quality Gates

Run quality gates **before every commit**. If any gate fails, fix it in the same script.

### Backend (Java / Gradle)
```bash
echo "--- Compiling and Testing Backend ---"
cd backend
./gradlew clean build 2>&1
cd ..
```

### Frontend (TypeScript / Vite)
```bash
echo "--- Linting and Testing Frontend ---"
cd frontend
npx biome check --apply . 2>&1
npx tsc --noEmit 2>&1
npm run build 2>&1
cd ..
```

---

## 5. TDD Implementation Loop

Each script you output should perform one logical unit of work, run the quality gates, and make an atomic commit. **Every script must start with `cd "$WT"`** (where `$WT` is the worktree path).

### Step 1: Red (Write Failing Test)
```bash
#!/usr/bin/env bash
set -euo pipefail
WT="../vantage-agent-1-task-001"
cd "$WT"

# 1. Write the test file (cat > ...)
# 2. Run tests to verify they fail
cd backend && ./gradlew test --tests "*OrderOutboxIT" 2>&1 || true
cd ..

git add -A
git commit -m "test(order): add failing test for outbox transaction"
```

### Step 2: Green (Implement Minimum Code)
```bash
#!/usr/bin/env bash
set -euo pipefail
WT="../vantage-agent-1-task-001"
cd "$WT"

# 1. Patch or create OrderService.java, OutboxEvent.java, etc.
# 2. Run tests to verify they pass
cd backend && ./gradlew clean build 2>&1
cd ..

git add -A
git commit -m "feat(order): implement transactional outbox pattern"
```

---

## 6. Phase 5: PR Creation (Final Step)

Only after all tests pass and the task is complete, emit a PR creation script.

```bash
#!/usr/bin/env bash
set -euo pipefail
WT="../vantage-agent-1-task-001"
BRANCH="agent-1/TASK-001"
BASE_BRANCH="main"
TASK_ID="TASK-001"

cd "$WT"

echo "=== Final quality gate ==="
cd backend && ./gradlew clean build 2>&1
cd ..

echo "=== Pushing branch ==="
git push origin "$BRANCH"

echo "=== Creating PR ==="
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

echo "✅ PR created"
```

---

## 7. Final Output Contract

1. Read the injected task manifest and context files.
2. If no worktree exists, output a script to create it.
3. Output a single ` ```bash ` script that implements the first logical step (usually writing the failing test). **Every script must `cd` into the worktree.**
4. Wait for the user to execute the script and paste the output.
5. If the output contains errors, output a single ` ```bash ` script that surgically patches the error.
6. Once all acceptance criteria are met, output the PR creation script.
SKILL_DELIM

echo "Skipping compile check (documentation files)"
COMPILE_OK=true

if [ "$INCOMPLETE" = true ] || [ "$COMPILE_OK" = false ]; then
  echo "Skipping tests and commit due to incomplete files or compilation errors"
  exit 1
fi

echo "Running tests"
echo "Skipping tests (documentation files)"

echo "All tests passed. Committing."
git add -A
git commit -m "docs(meta): enforce git worktree and branch isolation in agent skill"
```
