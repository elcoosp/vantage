#!/usr/bin/env bash
set -euo pipefail

# ----------------------------------------------------------------------
# Vantage End-to-End Smoke Test
# ----------------------------------------------------------------------
# This script verifies the entire platform by starting infrastructure,
# booting backend and frontend, executing the 90-second demo scenario,
# running quality gates, and cleaning up.
# ----------------------------------------------------------------------

# ----- configuration --------------------------------------------------
BACKEND_PORT=8080
FRONTEND_PORT=5173
BACKEND_HEALTH_URL="http://localhost:${BACKEND_PORT}/actuator/health"
PROMETHEUS_URL="http://localhost:${BACKEND_PORT}/actuator/prometheus"
API_BASE="http://localhost:${BACKEND_PORT}/api/v1"
FRONTEND_URL="http://localhost:${FRONTEND_PORT}"

DOCKER_COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
  DOCKER_COMPOSE_CMD="docker compose"
fi

# ----- helper functions ------------------------------------------------
log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

wait_for_http() {
  local url="$1"
  local name="$2"
  local max_attempts=90
  local delay=2
  log "Waiting for $name at $url"
  for ((i=1; i<=max_attempts; i++)); do
    if curl -s -f -o /dev/null "$url"; then
      log "$name is ready"
      return 0
    fi
    sleep "$delay"
  done
  log "ERROR: $name did not become ready within $((max_attempts * delay)) seconds"
  return 1
}

wait_for_postgres() {
  log "Waiting for PostgreSQL to be healthy"
  local max_attempts=60
  local delay=2
  local container="$(${DOCKER_COMPOSE_CMD} ps -q postgres)"

  # Wait for the container to be ready with the 'vantage' user
  for ((i=1; i<=max_attempts; i++)); do
    if docker exec "$container" pg_isready -U vantage &> /dev/null; then
      log "PostgreSQL is ready with user 'vantage'"
      break
    fi
    sleep "$delay"
  done

  sleep 2

  log "Creating vantage_primary and vantage_replica if not exists"
  docker exec "$container" psql -U vantage -d postgres -c "CREATE DATABASE vantage_primary;" 2>/dev/null || true
  docker exec "$container" psql -U vantage -d postgres -c "CREATE DATABASE vantage_replica;" 2>/dev/null || true

  log "Verifying connection to vantage_primary as 'vantage'"
  for ((i=1; i<=max_attempts; i++)); do
    if docker exec "$container" psql -U vantage -d vantage_primary -c "SELECT 1" &> /dev/null; then
      log "PostgreSQL is healthy and configured"
      return 0
    fi
    sleep "$delay"
  done
  log "ERROR: PostgreSQL did not become healthy with configured user"
  return 1
}

wait_for_rabbitmq() {
  log "Waiting for RabbitMQ to be healthy"
  local max_attempts=60
  local delay=2
  for ((i=1; i<=max_attempts; i++)); do
    if docker exec "$(${DOCKER_COMPOSE_CMD} ps -q rabbitmq)" rabbitmqctl status &> /dev/null; then
      log "RabbitMQ is healthy"
      return 0
    fi
    sleep "$delay"
  done
  log "ERROR: RabbitMQ did not become healthy"
  return 1
}

# ----- cleanup trap ----------------------------------------------------
cleanup() {
  local exit_code=$?
  log "Cleaning up..."
  if [ -n "${BACKEND_PID:-}" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    log "Stopping backend (PID $BACKEND_PID)"
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
  if [ -n "${FRONTEND_PID:-}" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    log "Stopping frontend (PID $FRONTEND_PID)"
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
  log "Shutting down docker-compose"
  ${DOCKER_COMPOSE_CMD} down -v 2>/dev/null || true
  if [ $exit_code -ne 0 ]; then
    log "Script failed with exit code $exit_code. Dumping logs..."
    log "--- backend.log (last 100 lines) ---"
    tail -n 100 backend.log 2>/dev/null || echo "No backend.log"
    log "--- frontend.log (last 50 lines) ---"
    tail -n 50 frontend.log 2>/dev/null || echo "No frontend.log"
    log "--- PostgreSQL logs (last 50 lines) ---"
    docker logs "$(${DOCKER_COMPOSE_CMD} ps -q postgres)" --tail 50 2>&1 | while read line; do log "  $line"; done
  fi
}
trap cleanup EXIT

# ----- start infrastructure --------------------------------------------
log "Starting infrastructure via docker-compose"
${DOCKER_COMPOSE_CMD} down -v --remove-orphans 2>/dev/null || true
${DOCKER_COMPOSE_CMD} rm -f 2>/dev/null || true
${DOCKER_COMPOSE_CMD} up -d

wait_for_postgres
wait_for_rabbitmq

# ----- start backend ---------------------------------------------------
log "Starting Spring Boot backend"
cd backend

# Use --args to pass datasource properties directly to Spring Boot
SPRING_PROFILES_ACTIVE=smoke-test ./gradlew bootRun --no-daemon -Dspring.datasource.primary.url=jdbc:postgresql://localhost:5432/vantage_primary -Dspring.datasource.primary.username=vantage -Dspring.datasource.primary.password=vantage_pw -Dspring.datasource.replica.url=jdbc:postgresql://localhost:5432/vantage_replica -Dspring.datasource.replica.username=vantage -Dspring.datasource.replica.password=vantage_pw > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..
log "Backend PID: $BACKEND_PID"

log "Waiting 15 seconds for initial startup..."
sleep 15

if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
  log "ERROR: Backend process died. Dumping backend.log:"
  cat backend.log 2>/dev/null || echo "No backend.log"
  exit 1
fi

wait_for_http "$BACKEND_HEALTH_URL" "Backend health endpoint"

# ----- start frontend --------------------------------------------------
log "Starting Vite frontend dev server"
cd frontend
if [ ! -d "node_modules" ]; then
  log "Installing frontend dependencies"
  npm install --no-fund --no-audit
fi
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

wait_for_http "$FRONTEND_URL" "Frontend dev server"

# ----- execute demo scenario -------------------------------------------
log "Starting demo scenario"

# Helper: POST JSON and extract field
post_json() {
  local url="$1"
  local data="$2"
  local header="${3:-}"
  local extra_args=()
  if [ -n "$header" ]; then
    extra_args=(-H "$header")
  fi
  curl -s -X POST "${extra_args[@]}" -H "Content-Type: application/json" -d "$data" "$url"
}

# 1. Register Vendor A and Vendor B
log "Registering Vendor A"
REG_A=$(post_json "${API_BASE}/vendors/register" '{"email":"venda@vantage.com","password":"securePassword123","name":"Vendor A"}')
TOKEN_A=$(echo "$REG_A" | jq -r '.accessToken')
TENANT_A=$(echo "$REG_A" | jq -r '.tenantId')

log "Registering Vendor B"
REG_B=$(post_json "${API_BASE}/vendors/register" '{"email":"vendb@vantage.com","password":"securePassword123","name":"Vendor B"}')
TOKEN_B=$(echo "$REG_B" | jq -r '.accessToken')
TENANT_B=$(echo "$REG_B" | jq -r '.tenantId')

if [ -z "$TOKEN_A" ] || [ -z "$TENANT_A" ]; then
  log "ERROR: Vendor A registration failed"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer $TOKEN_A"
TENANT_HEADER="X-Tenant-ID: $TENANT_A"

# 3. Create a Product
log "Creating product"
PRODUCT_DATA='{"name":"Smoke Test Product","price":99.99,"description":"E2E test product"}'
PRODUCT_RESP=$(post_json "${API_BASE}/products" "$PRODUCT_DATA" "$AUTH_HEADER" -H "$TENANT_HEADER")
PRODUCT_ID=$(echo "$PRODUCT_RESP" | jq -r '.id')
if [ -z "$PRODUCT_ID" ] || [ "$PRODUCT_ID" = "null" ]; then
  log "ERROR: Product creation failed"
  exit 1
fi

# 4. Initialize Inventory to 1 (using If-Match: 0)
log "Initializing inventory to 1"
INV_DATA='{"quantity":1}'
curl -s -X PUT -H "$AUTH_HEADER" -H "$TENANT_HEADER" -H "If-Match: 0" -H "Content-Type: application/json" -d "$INV_DATA" "${API_BASE}/inventory/${PRODUCT_ID}" > /dev/null

# 5. Enable Chaos Monkey (payment failure)
log "Enabling Chaos Monkey"
CHAOS_DATA='{"enabled":true}'
curl -s -X POST -H "$AUTH_HEADER" -H "$TENANT_HEADER" -H "Content-Type: application/json" -d "$CHAOS_DATA" "${API_BASE}/admin/chaos-monkey/payment-failure" > /dev/null

# 6. Place an Order (first, will fail)
log "Placing first order (expecting cancellation)"
ORDER_DATA="{\"productId\":\"$PRODUCT_ID\",\"quantity\":1,\"productName\":\"Smoke Test Product\"}"
ORDER_RESP=$(post_json "${API_BASE}/orders" "$ORDER_DATA" "$AUTH_HEADER" -H "$TENANT_HEADER")
ORDER_ID=$(echo "$ORDER_RESP" | jq -r '.orderId')
if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
  log "ERROR: Order creation failed"
  exit 1
fi

# 7. Poll order status until CANCELLED
log "Polling order status until CANCELLED"
status=""
for ((i=1; i<=30; i++)); do
  search_resp=$(curl -s -H "$AUTH_HEADER" -H "$TENANT_HEADER" "${API_BASE}/orders/search?size=100")
  status=$(echo "$search_resp" | jq -r --arg oid "$ORDER_ID" '.content[] | select(.orderId==$oid) | .status')
  if [ "$status" = "CANCELLED" ]; then
    log "Order $ORDER_ID is CANCELLED"
    break
  fi
  log "Current status: $status (waiting for CANCELLED)"
  sleep 2
done

if [ "$status" != "CANCELLED" ]; then
  log "ERROR: Order did not become CANCELLED within timeout"
  exit 1
fi

# 8. Verify inventory is back to 1
log "Verifying inventory is restored to 1"
INV_QUANT=$(docker exec "$(${DOCKER_COMPOSE_CMD} ps -q postgres)" psql -U vantage -d vantage_primary -t -c "SELECT quantity FROM inventory WHERE product_id = '$PRODUCT_ID';" | tr -d ' ')
if [ "$INV_QUANT" != "1" ]; then
  log "ERROR: Inventory quantity is $INV_QUANT, expected 1"
  exit 1
fi
log "Inventory is 1"

# 9. Disable Chaos Monkey
log "Disabling Chaos Monkey"
CHAOS_DISABLE='{"enabled":false}'
curl -s -X POST -H "$AUTH_HEADER" -H "$TENANT_HEADER" -H "Content-Type: application/json" -d "$CHAOS_DISABLE" "${API_BASE}/admin/chaos-monkey/payment-failure" > /dev/null

# 10. Place second order (should succeed)
log "Placing second order (expecting PAID)"
ORDER_RESP2=$(post_json "${API_BASE}/orders" "$ORDER_DATA" "$AUTH_HEADER" -H "$TENANT_HEADER")
ORDER_ID2=$(echo "$ORDER_RESP2" | jq -r '.orderId')
if [ -z "$ORDER_ID2" ] || [ "$ORDER_ID2" = "null" ]; then
  log "ERROR: Second order creation failed"
  exit 1
fi

log "Polling order status until PAID"
status2=""
for ((i=1; i<=30; i++)); do
  search_resp2=$(curl -s -H "$AUTH_HEADER" -H "$TENANT_HEADER" "${API_BASE}/orders/search?size=100")
  status2=$(echo "$search_resp2" | jq -r --arg oid "$ORDER_ID2" '.content[] | select(.orderId==$oid) | .status')
  if [ "$status2" = "PAID" ]; then
    log "Order $ORDER_ID2 is PAID"
    break
  fi
  log "Current status: $status2 (waiting for PAID)"
  sleep 2
done

if [ "$status2" != "PAID" ]; then
  log "ERROR: Second order did not become PAID within timeout"
  exit 1
fi

# 11. Verify Prometheus metrics endpoint has metrics
log "Checking Prometheus metrics endpoint"
if ! curl -s -f "$PROMETHEUS_URL" | grep -q "vantage_"; then
  log "ERROR: Prometheus endpoint did not return expected metrics"
  exit 1
fi
log "Prometheus metrics OK"

# ----- run test suite and build -----------------------------------------
log "Running backend test suite"
cd backend
./gradlew qualityGate
cd ..

log "Building frontend"
cd frontend
npm run build
cd ..

# ----- success ---------------------------------------------------------
log "All smoke test steps passed successfully!"
exit 0