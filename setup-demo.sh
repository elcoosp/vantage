#!/usr/bin/env bash
#
# ============================================================================
#  Vantage — Demo Environment Bootstrap
# ============================================================================
#  One command to make the app recordable:
#      ./setup-demo.sh                # start everything (Docker + backend + frontend)
#      ./setup-demo.sh start --reset  # wipe DB volumes first -> pristine seed take
#      ./setup-demo.sh stop           # stop backend/frontend + docker compose stop
#      ./setup-demo.sh status         # is everything up?
#      ./setup-demo.sh logs [backend|frontend]
#
#  What "start" does, in order:
#    1. Preflight checks      docker daemon, compose cmd, repo layout, Java 21, npm, curl
#    2. (--reset only)        docker compose down -v  -> fresh DB volumes
#    3. Docker infra          postgres (host :15432) + rabbitmq (:5672)
#    4. Postgres prep         waits pg_isready, creates vantage_primary + vantage_replica
#    5. Backend               ./gradlew bootRun  (Spring Boot :8080, Flyway seeds demo data)
#    6. Seed verification     vendors row for admin@vantage.com + real login API call
#    7. Frontend              npm install (if needed) + vite dev server :5173
#    8. Prints a record-ready summary
#
#  Demo credentials (seeded by Flyway migration V8):
#      admin@vantage.com / password123
#
#  Logs & pids land in ./logs/  (add "logs/" to .gitignore)
#
#  Works on macOS (bash 3.2) and Linux. For Windows use Git Bash or WSL.
# ============================================================================
set -euo pipefail

# ----- configuration --------------------------------------------------------
BACKEND_PORT=8080
FRONTEND_PORT=5173
PG_HOST_PORT=15432
RABBIT_HOST_PORT=5672
DEMO_EMAIL="admin@vantage.com"
DEMO_PASSWORD="password123"
LOGIN_URL="http://localhost:${BACKEND_PORT}/api/v1/vendors/login"
FRONTEND_URL="http://localhost:${FRONTEND_PORT}"

BACKEND_WAIT_ATTEMPTS=150   # x3s = 7.5 min (first run may download Gradle deps)
TABLE_WAIT_ATTEMPTS=60      # x2s = 2 min  (Flyway migrations)
FRONTEND_WAIT_ATTEMPTS=60   # x2s = 2 min

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${ROOT}/logs"
BACKEND_LOG="${LOG_DIR}/backend.log"
FRONTEND_LOG="${LOG_DIR}/frontend.log"
BACKEND_PID_FILE="${LOG_DIR}/backend.pid"
FRONTEND_PID_FILE="${LOG_DIR}/frontend.pid"

# ----- pretty output --------------------------------------------------------
if [ -t 1 ]; then
  C_G=$'\033[32m'; C_R=$'\033[31m'; C_Y=$'\033[33m'; C_B=$'\033[36m'; C_N=$'\033[0m'
else
  C_G=""; C_R=""; C_Y=""; C_B=""; C_N=""
fi
log()  { echo "[$(date +'%H:%M:%S')] $*"; }
ok()   { echo "${C_G}  ✔ ${C_N}$*"; }
warn() { echo "${C_Y}  ⚠ ${C_N}$*"; }
fail() { echo "${C_R}  ✘ ${C_N}$*" >&2; }

# ----- generic helpers ------------------------------------------------------
# TCP port probe without external deps (bash /dev/tcp)
port_open() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# HTTP status code of a URL, "000" when unreachable
http_code() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$1" 2>/dev/null || echo 000
}

kill_tree() {
  # Recursively kill a process and its children (gradle wrapper -> bootRun JVM)
  local pid="$1" kids kid
  kids="$(pgrep -P "$pid" 2>/dev/null || true)"
  for kid in $kids; do kill_tree "$kid"; done
  kill "$pid" 2>/dev/null || true
}

wait_for_postgres() {
  local container="$1" i
  log "Waiting for PostgreSQL to accept connections..."
  for ((i = 1; i <= 60; i++)); do
    if docker exec "$container" pg_isready -U vantage >/dev/null 2>&1; then
      ok "PostgreSQL is up (host port ${PG_HOST_PORT})"
      return 0
    fi
    sleep 2
  done
  fail "PostgreSQL did not become ready in 120s. Check: docker compose logs postgres"
  return 1
}

ensure_databases() {
  local container="$1" db
  log "Ensuring databases vantage_primary / vantage_replica exist..."
  for db in vantage_primary vantage_replica; do
    if docker exec "$container" psql -U vantage -d postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname='${db}'" 2>/dev/null | grep -q 1; then
      ok "database '${db}' already exists"
    else
      docker exec "$container" psql -U vantage -d postgres -c "CREATE DATABASE ${db};" >/dev/null
      ok "database '${db}' created"
    fi
  done
}

wait_for_rabbitmq() {
  local container="$1" i
  log "Waiting for RabbitMQ (first boot can take ~30s)..."
  for ((i = 1; i <= 60; i++)); do
    if docker exec "$container" rabbitmqctl status >/dev/null 2>&1; then
      ok "RabbitMQ is up (AMQP :${RABBIT_HOST_PORT}, management UI http://localhost:15672)"
      return 0
    fi
    sleep 2
  done
  fail "RabbitMQ did not become healthy in 120s. Check: docker compose logs rabbitmq"
  return 1
}

# ----- preflight ------------------------------------------------------------
preflight() {
  log "Running preflight checks..."

  command -v docker >/dev/null 2>&1 || { fail "docker not found. Install Docker Desktop (macOS/Windows) or docker.io (Linux)."; exit 1; }
  if ! docker info >/dev/null 2>&1; then
    fail "Docker daemon is not running. Start Docker Desktop / the docker service, then re-run."
    exit 1
  fi
  ok "docker daemon running"

  if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE=(docker-compose)
  else
    fail "Neither 'docker compose' (v2 plugin) nor legacy 'docker-compose' found."
    exit 1
  fi
  ok "compose command: ${COMPOSE[*]}"

  [ -f "${ROOT}/docker-compose.yml" ] || { fail "docker-compose.yml not found in ${ROOT} — put this script in the repo root."; exit 1; }
  [ -f "${ROOT}/backend/gradlew" ]    || { fail "backend/gradlew not found — run this from the vantage repo root."; exit 1; }
  [ -f "${ROOT}/frontend/package.json" ] || { fail "frontend/package.json not found — run this from the vantage repo root."; exit 1; }
  [ -x "${ROOT}/backend/gradlew" ] || chmod +x "${ROOT}/backend/gradlew"
  ok "repo layout OK (docker-compose.yml, backend/, frontend/)"

  if [ -n "${JAVA_HOME:-}" ]; then
    ok "JAVA_HOME is set (${JAVA_HOME})"
  elif command -v java >/dev/null 2>&1; then
    local jv
    jv="$(java -version 2>&1 | head -n 1)"
    echo "$jv" | grep -q 'version "21\.' || warn "Java 21 is required (README badge) — found: ${jv}. Continuing, but bootRun may fail."
    ok "java found: ${jv}"
  else
    fail "No java on PATH and JAVA_HOME is unset. Install Java 21 (e.g. from adoptium.net)."
    exit 1
  fi

  command -v curl >/dev/null 2>&1 || { fail "curl not found (required for health checks)."; exit 1; }
  if ! command -v npm >/dev/null 2>&1; then
    if [ -d "${ROOT}/frontend/node_modules" ]; then
      warn "npm not found, but frontend/node_modules exists — will try to start without installing."
    else
      fail "npm not found and frontend dependencies are not installed. Install Node.js (with npm) first."
      exit 1
    fi
  fi
  ok "curl available"
}

# ----- port guards (idempotent re-runs) --------------------------------------
check_port_backend() {
  if port_open "$BACKEND_PORT"; then
    if [ "$(http_code "$LOGIN_URL")" != "000" ]; then
      BACKEND_ALREADY_UP=1
      warn "port ${BACKEND_PORT} busy but something answers — assuming an existing backend, will reuse."
    else
      fail "port ${BACKEND_PORT} is in use by another process (not a Vantage backend). Free the port, then re-run."
      exit 1
    fi
  fi
}

check_port_frontend() {
  if port_open "$FRONTEND_PORT"; then
    if [ "$(http_code "$FRONTEND_URL")" != "000" ]; then
      FRONTEND_ALREADY_UP=1
      warn "port ${FRONTEND_PORT} busy but something answers — assuming an existing frontend, will reuse."
    else
      fail "port ${FRONTEND_PORT} is in use by another process (not a Vite server). Free the port, then re-run."
      exit 1
    fi
  fi
}

# ----- app lifecycle ----------------------------------------------------------
start_backend() {
  if [ "${BACKEND_ALREADY_UP:-0}" = "1" ]; then
    ok "Backend already running on :${BACKEND_PORT} — reusing it."
    return 0
  fi
  log "Starting Spring Boot backend (./gradlew bootRun)..."
  log "First run may take several minutes while Gradle downloads dependencies."
  cd "${ROOT}/backend"
  nohup ./gradlew bootRun --no-daemon > "${BACKEND_LOG}" 2>&1 &
  BACKEND_PID=$!
  echo "${BACKEND_PID}" > "${BACKEND_PID_FILE}"
  cd "${ROOT}"

  local i code
  log "Waiting for backend to accept requests..."
  for ((i = 1; i <= BACKEND_WAIT_ATTEMPTS; i++)); do
    if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
      fail "Backend process exited during startup. Last log lines:"
      tail -n 60 "${BACKEND_LOG}" || true
      exit 1
    fi
    code="$(http_code "$LOGIN_URL")"
    if [ "${code}" != "000" ]; then
      ok "Backend is responding (HTTP ${code}) — pid ${BACKEND_PID}"
      return 0
    fi
    sleep 3
  done
  fail "Backend did not come up in $((BACKEND_WAIT_ATTEMPTS * 3 / 60)) min. Last log lines:"
  tail -n 60 "${BACKEND_LOG}" || true
  exit 1
}

verify_seed_and_login() {
  local pg_container="$1" i code seed_count
  local payload="{\"email\":\"${DEMO_EMAIL}\",\"password\":\"${DEMO_PASSWORD}\"}"

  log "Waiting for Flyway migrations to finish (vendors table)..."
  for ((i = 1; i <= TABLE_WAIT_ATTEMPTS; i++)); do
    seed_count="$(docker exec "$pg_container" psql -U vantage -d vantage_primary -tAc \
      "SELECT to_regclass('public.vendors') IS NOT NULL" 2>/dev/null || echo f)"
    if [ "${seed_count}" = "t" ]; then break; fi
    sleep 2
  done
  if [ "${seed_count:-f}" != "t" ]; then
    fail "vendors table missing after backend start — migrations did not run. See ${BACKEND_LOG}"
    exit 1
  fi
  ok "Flyway migrations applied (vendors table present)"

  seed_count="$(docker exec "$pg_container" psql -U vantage -d vantage_primary -tAc \
    "SELECT count(*) FROM vendors WHERE email='${DEMO_EMAIL}'" 2>/dev/null || echo 0)"
  if [ "${seed_count}" != "1" ]; then
    fail "Seed admin (${DEMO_EMAIL}) NOT found in vantage_primary (found: ${seed_count})."
    echo "      The demo data is stale or was consumed by previous runs." >&2
    echo "      Fix:  ./setup-demo.sh start --reset   (wipes DB volumes and re-seeds)" >&2
    exit 1
  fi
  ok "Seed admin row present"

  log "Verifying demo login via API (POST /api/v1/vendors/login)..."
  for ((i = 1; i <= 10; i++)); do
    code="$(curl -s -o "${LOG_DIR}/login-check.json" -w '%{http_code}' --max-time 5 \
      -X POST "$LOGIN_URL" -H 'Content-Type: application/json' -d "${payload}" 2>/dev/null || echo 000)"
    if [ "${code}" = "200" ] && grep -q '"token"' "${LOG_DIR}/login-check.json" 2>/dev/null; then
      ok "Login verified: ${DEMO_EMAIL} / ${DEMO_PASSWORD} -> JWT received"
      return 0
    fi
    sleep 3
  done
  fail "Login failed (HTTP ${code:-?}). Response body:"
  cat "${LOG_DIR}/login-check.json" 2>/dev/null || true
  echo "" >&2
  tail -n 40 "${BACKEND_LOG}" || true
  exit 1
}

start_frontend() {
  if [ "${FRONTEND_ALREADY_UP:-0}" = "1" ]; then
    ok "Frontend already running on :${FRONTEND_PORT} — reusing it."
    return 0
  fi
  cd "${ROOT}/frontend"
  if [ ! -d node_modules ]; then
    log "Installing frontend dependencies (npm install)..."
    npm install --no-fund --no-audit
  fi
  log "Starting Vite dev server on :${FRONTEND_PORT}..."
  nohup npm run dev -- --strictPort > "${FRONTEND_LOG}" 2>&1 &
  FRONTEND_PID=$!
  echo "${FRONTEND_PID}" > "${FRONTEND_PID_FILE}"
  cd "${ROOT}"

  local i body
  log "Waiting for frontend..."
  for ((i = 1; i <= FRONTEND_WAIT_ATTEMPTS; i++)); do
    if ! kill -0 "${FRONTEND_PID}" 2>/dev/null; then
      fail "Frontend process exited during startup. Last log lines:"
      tail -n 40 "${FRONTEND_LOG}" || true
      exit 1
    fi
    body="$(curl -s --max-time 3 "$FRONTEND_URL" 2>/dev/null || true)"
    if echo "${body}" | grep -q 'id="root"'; then
      ok "Frontend is up — http://localhost:${FRONTEND_PORT} (pid ${FRONTEND_PID})"
      return 0
    fi
    sleep 2
  done
  fail "Frontend did not come up. Last log lines:"
  tail -n 40 "${FRONTEND_LOG}" || true
  exit 1
}

# ----- commands ----------------------------------------------------------------
do_stop() {
  log "Stopping backend and frontend..."
  local pid_file pid
  for pid_file in "${BACKEND_PID_FILE}" "${FRONTEND_PID_FILE}"; do
    if [ -f "$pid_file" ]; then
      pid="$(cat "$pid_file" 2>/dev/null || true)"
      if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        log "Killing process tree of pid ${pid} ($(basename "$pid_file" .pid))"
        kill_tree "$pid"
      else
        warn "stale pid file $(basename "$pid_file") — removing"
      fi
      rm -f "$pid_file"
    fi
  done
  sleep 2
  # last-resort: free the ports if something is still bound
  if port_open "$BACKEND_PORT"; then warn "port ${BACKEND_PORT} still busy — check 'lsof -i :${BACKEND_PORT}'"; fi
  if port_open "$FRONTEND_PORT"; then warn "port ${FRONTEND_PORT} still busy — check 'lsof -i :${FRONTEND_PORT}'"; fi

  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    log "Stopping docker compose services (database volumes are KEPT)..."
    if docker compose version >/dev/null 2>&1; then
      docker compose stop
    elif command -v docker-compose >/dev/null 2>&1; then
      docker-compose stop
    fi
    ok "docker services stopped (data preserved for next run)"
  fi
  ok "All stopped. Fresh take next time:  ./setup-demo.sh start --reset"
}

do_status() {
  local rc=0 pg rmq
  echo "Vantage demo stack status:"
  if docker info >/dev/null 2>&1; then
    pg="$("${COMPOSE[@]}" ps -q postgres 2>/dev/null || true)"
    rmq="$("${COMPOSE[@]}" ps -q rabbitmq 2>/dev/null || true)"
    if [ -n "$pg" ] && docker exec "$pg" pg_isready -U vantage >/dev/null 2>&1; then
      ok "postgres   up (:${PG_HOST_PORT})"
    else echo "${C_R}  ✘ ${C_N}postgres   down"; rc=1; fi
    if [ -n "$rmq" ] && docker exec "$rmq" rabbitmqctl status >/dev/null 2>&1; then
      ok "rabbitmq   up (:${RABBIT_HOST_PORT})"
    else echo "${C_R}  ✘ ${C_N}rabbitmq   down"; rc=1; fi
  else
    echo "${C_R}  ✘ ${C_N}docker     daemon not running"; rc=1
  fi
  if [ "$(http_code "$LOGIN_URL")" != "000" ]; then
    ok "backend    responding (:${BACKEND_PORT})"
  else echo "${C_R}  ✘ ${C_N}backend    down"; rc=1; fi
  if [ "$(http_code "$FRONTEND_URL")" != "000" ]; then
    ok "frontend   responding (:${FRONTEND_PORT})"
  else echo "${C_R}  ✘ ${C_N}frontend   down"; rc=1; fi
  exit $rc
}

do_start() {
  : "${COMPOSE[0]:-}" # set -u guard
  BACKEND_ALREADY_UP=0
  FRONTEND_ALREADY_UP=0

  preflight
  check_port_backend
  check_port_frontend

  if [ "${RESET:-0}" = "1" ]; then
    log "Reset requested — wiping docker volumes (down -v) for a pristine seed..."
    do_stop_quiet
    if port_open "$BACKEND_PORT" || port_open "$FRONTEND_PORT"; then
      fail "Ports ${BACKEND_PORT}/${FRONTEND_PORT} are still held by processes this script did not start."
      echo "      Stop them (lsof -i :${BACKEND_PORT} / :${FRONTEND_PORT}) and re-run ./setup-demo.sh start --reset" >&2
      exit 1
    fi
    "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
    # stale reuse flags from the port pre-check no longer apply after the wipe
    BACKEND_ALREADY_UP=0
    FRONTEND_ALREADY_UP=0
    ok "volumes wiped — Flyway will re-seed demo data on backend boot"
  fi

  log "Starting docker infra (postgres + rabbitmq)..."
  "${COMPOSE[@]}" up -d postgres rabbitmq
  local pg_container rmq_container
  pg_container="$("${COMPOSE[@]}" ps -q postgres)"
  rmq_container="$("${COMPOSE[@]}" ps -q rabbitmq)"
  [ -n "$pg_container" ] || { fail "postgres container did not start. Check: docker compose logs postgres"; exit 1; }
  wait_for_postgres "$pg_container"
  ensure_databases "$pg_container"
  wait_for_rabbitmq "$rmq_container"

  start_backend
  verify_seed_and_login "$pg_container"
  start_frontend

  echo ""
  echo "${C_B}════════════════════════════════════════════════════════════${C_N}"
  echo "${C_G}  ✔ VANTAGE IS READY TO RECORD${C_N}"
  echo "${C_B}════════════════════════════════════════════════════════════${C_N}"
  echo "   App          http://localhost:${FRONTEND_PORT}"
  echo "   Demo login   ${DEMO_EMAIL} / ${DEMO_PASSWORD}"
  echo "   Backend API  http://localhost:${BACKEND_PORT}  (login verified ✔)"
  echo "   Logs         logs/backend.log · logs/frontend.log"
  echo "   Stop         ./setup-demo.sh stop"
  echo "   Fresh take   ./setup-demo.sh start --reset"
  echo ""
  echo "   Record tips:"
  echo "   • Clear browser site data / use a fresh profile so /login shows."
  echo "   • 1920×1080 viewport, 100% zoom, hide bookmarks bar, close notifications."
  echo "   • --reset wipes DB state: do it between takes for identical demos."
  echo "${C_B}════════════════════════════════════════════════════════════${C_N}"
}

do_stop_quiet() {
  local pid
  for pid in "$(cat "${BACKEND_PID_FILE}" 2>/dev/null || true)" \
             "$(cat "${FRONTEND_PID_FILE}" 2>/dev/null || true)"; do
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && kill_tree "$pid" || true
  done
  rm -f "${BACKEND_PID_FILE}" "${FRONTEND_PID_FILE}"
  sleep 2
}

do_logs() {
  mkdir -p "$LOG_DIR"
  local which="${1:-all}"
  case "$which" in
    backend)  tail -n 60 -f "${BACKEND_LOG}" ;;
    frontend) tail -n 60 -f "${FRONTEND_LOG}" ;;
    all)      tail -n 40 -f "${BACKEND_LOG}" "${FRONTEND_LOG}" ;;
    *) fail "usage: $0 logs [backend|frontend|all]"; exit 1 ;;
  esac
}

usage() {
  sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# ----- main -------------------------------------------------------------------
mkdir -p "$LOG_DIR"
CMD="${1:-start}"
case "$CMD" in
  start)
    shift || true
    for arg in "$@"; do
      case "$arg" in
        --reset|-r) RESET=1 ;;
        *) fail "unknown option: $arg"; exit 1 ;;
      esac
    done
    do_start
    ;;
  stop)    do_stop ;;
  status)  COMPOSE=(docker compose); docker compose version >/dev/null 2>&1 || COMPOSE=(docker-compose); do_status ;;
  logs)    shift || true; do_logs "${*:-all}" ;;
  help|-h|--help) usage ;;
  *)       fail "unknown command: $CMD (use: start | stop | status | logs | help)"; exit 1 ;;
esac
