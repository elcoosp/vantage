# TASK-011: Implement Live Ops Shipping Map (WebSockets & Geocoding)

## Product Context
- Read: `docs/00-product/02-features-and-business-rules.md` (Section: Real-Time Operations)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 6.1: Live Ops Shipping Map)

## Objective
Implement real-time WebSocket infrastructure to push shipped order events to a frontend dashboard. When an order reaches the `SHIPPED` state, the backend must resolve the shipping city to lat/long coordinates using the free Nominatim geocoding API, and broadcast a map pin payload to all connected clients for that tenant.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/websocket/`, `backend/src/main/java/com/vantage/order/` (to emit the event), `backend/src/main/java/com/vantage/integration/` (for geocoding), and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or any files in `frontend/`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/01-async-events-spec.yaml` (Reference: `PaymentSucceededEvent`)
- `backend/src/main/java/com/vantage/order/domain/Order.java`
- `backend/src/main/java/com/vantage/order/domain/OrderStatus.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEvent.java`
- `backend/src/main/java/com/vantage/core/messaging/domain/ProcessedEventRepository.java`

## Acceptance Criteria

### 1. WebSocket Configuration
1. Create `WebSocketConfig` in `com.vantage.core.websocket` implementing `WebSocketMessageBrokerConfigurer`.
2. Enable the STOMP relay endpoint at `/ws`.
3. Set the application destination prefix to `/app`.
4. Set the broker prefix to `/topic`.
5. Register an intercepter to extract the `tenantId` from the STOMP CONNECT headers (or rely on the JWT query param) to restrict map pin broadcasts to the correct tenant.

### 2. Geocoding Integration
1. Create `NominatimGeocodingClient` in `com.vantage.integration.infrastructure`.
2. Use `RestClient` (or `RestTemplate`) to call the Nominatim API: `https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1`.
3. Parse the response to extract `lat` and `lon` as `Double`.
4. Apply Resilience4j `@CircuitBreaker` and `@Retry` to handle Nominatim rate limits or downtime.

### 3. Shipping Event Consumer & Broadcast
1. Create `OpsMapConsumer` in `com.vantage.core.websocket.messaging`.
2. Listen to `vantage.order.events` for `OrderShippedEvent` (Note: The Saga must be extended to emit this event when status transitions to `SHIPPED`. If `OrderShippedEvent` does not exist, listen to `PaymentSucceededEvent` and treat it as a trigger to mark the order as `SHIPPED` and proceed).
3. Check `ProcessedEventRepository` for idempotency.
4. Extract the shipping city from the payload (assume `city` is added to the `OrderCreatedPayload` and passed through the Saga, or use a default mock city if not present).
5. Call `NominatimGeocodingClient` to get coordinates.
6. Construct an `OpsMapPinPayload` containing `orderId`, `lat`, `lon`, and `city`.
7. Use `SimpMessagingTemplate` to broadcast the payload to `/topic/ops-map`.
8. Save the `eventId` to `processed_events`.

### 4. Integration Testing (Testcontainers)
1. Create `OpsMapWebSocketIT` in `backend/src/test/java/com/vantage/core/websocket/`.
2. Setup: Register a vendor, create a product, initialize inventory.
3. Connect a STOMP test client (e.g., using `WebSocketStompClient`) to `/ws` and subscribe to `/topic/ops-map`.
4. Place an order and simulate payment success to trigger the `SHIPPED` state.
5. Await at most 10 seconds.
6. Verify the STOMP client receives the `OpsMapPinPayload` containing the `orderId` and valid `lat`/`lon` coordinates.
7. Verify the `processed_events` table contains the event ID.

## Target File Paths
- `backend/src/main/java/com/vantage/core/websocket/config/WebSocketConfig.java`
- `backend/src/main/java/com/vantage/core/websocket/messaging/OpsMapConsumer.java`
- `backend/src/main/java/com/vantage/core/websocket/dto/OpsMapPinPayload.java`
- `backend/src/main/java/com/vantage/integration/infrastructure/NominatimGeocodingClient.java`
- `backend/src/main/java/com/vantage/integration/infrastructure/dto/NominatimResponse.java`
- `backend/src/test/java/com/vantage/core/websocket/OpsMapWebSocketIT.java`
