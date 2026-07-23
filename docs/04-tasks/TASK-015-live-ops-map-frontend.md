# TASK-015: Implement Live Ops Map Frontend (Leaflet.js & WebSockets)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Section: Persona 1 - The Vendor)
- User Story: `docs/00-product/03-epics-and-user-stories.md` (Story 6.1: Live Ops Shipping Map)

## Objective
Implement the real-time Live Ops dashboard in the React 19 frontend. The dashboard must connect to the backend WebSocket endpoint, subscribe to shipping events, and render a dark-mode world map using Leaflet.js. When a new order ships, a pulsing pin must drop onto the map at the provided coordinates.

## Execution Boundaries
- You may ONLY create or modify files inside the `frontend/` directory.
- DO NOT modify the `backend/` directory, `application.yml`, or `build.gradle.kts`.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `frontend/src/lib/api.ts`
- `frontend/src/components/Layout.tsx`

## Acceptance Criteria

### 1. Dependencies & Setup
1. Install `leaflet`, `react-leaflet`, and `@stomp/stompjs`.
2. Import Leaflet CSS in the application root (`main.tsx` or `App.tsx`).
3. Create a new route `/ops` in the application router.

### 2. WebSocket Integration
1. Create a custom hook `frontend/src/features/ops/useOpsMapSocket.ts`.
2. Configure the STOMP client to connect to `ws://localhost:8080/ws`.
3. Pass the JWT token in the connection headers (or query parameters, depending on backend WS security config).
4. Subscribe to the `/topic/ops-map` destination.
5. Maintain an array of map pins in React state. When a new `OpsMapPinPayload` is received, append it to the array.

### 3. Map Rendering
1. Create `frontend/src/features/ops/OpsDashboard.tsx`.
2. Render a `MapContainer` from `react-leaflet` centered at a default global view (e.g., latitude: 20, longitude: 0, zoom: 2).
3. Apply a dark-mode tile layer (e.g., CartoDB Dark Matter tiles: `https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png`).
4. Iterate over the pins array and render a `CircleMarker` or custom `Marker` for each pin at the provided `[lat, lon]`.
5. Add a pulsing CSS animation to the pins so they visually stand out when they first drop.

### 4. UI Polish
1. Display a counter in the top corner of the map: "Live Shipments: [X]".
2. If the WebSocket disconnects, show a "Reconnecting..." badge in the UI.

### 5. Verification (Output Instructions)
- Provide the terminal commands to install the new npm dependencies.
- Provide the command to run the frontend development server.

## Target File Paths
- `frontend/src/features/ops/OpsDashboard.tsx`
- `frontend/src/features/ops/MapView.tsx`
- `frontend/src/features/ops/useOpsMapSocket.ts`
- `frontend/src/index.css` (Modify to add Leaflet CSS import and pulse animations)
