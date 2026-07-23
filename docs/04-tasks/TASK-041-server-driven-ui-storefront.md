# TASK-041: Implement Server-Driven UI for Vendor Storefront

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Understand vendor customization needs)
- Read: `docs/00-product/03-epics-and-user-stories.md` (Frontend Wow Factor: Server-Driven UI)

## Objective
Implement a Server-Driven UI (SDUI) architecture. Instead of hardcoding the storefront layout in React, the Spring Boot backend will serve a JSON schema representing the UI components. The React 19 frontend will dynamically render these components based on the schema. This allows vendors to customize their storefront layout (e.g., reordering banners, product grids) from the backend without requiring a frontend redeploy, demonstrating advanced full-stack architecture.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/storefront/`, `frontend/src/features/storefront/`, and their corresponding test directories.
- DO NOT modify `application.yml`, `build.gradle.kts`, or other domain modules.

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `docs/02-contracts/02-rest-api-spec.yaml`
- `backend/src/main/java/com/vantage/product/domain/Product.java`

## Acceptance Criteria

### 1. Backend Storefront Schema Definition
1. Create `StorefrontConfig` entity in `com.vantage.storefront.domain` extending `BaseTenantEntity`.
   - Fields: `id` (UUID), `layoutPayload` (JSONB/String).
2. Create `StorefrontController` in `com.vantage.storefront.ui` exposing:
   - `GET /api/v1/storefront`: Returns the `layoutPayload` for the current tenant.
   - `PUT /api/v1/storefront`: Updates the `layoutPayload` (Admin only).
3. The `layoutPayload` must be a JSON array of component definitions. Example schema:
   ```json
   [
     { "componentType": "HeroBanner", "props": { "title": "Welcome to Vantage", "imageUrl": "..." } },
     { "componentType": "ProductGrid", "props": { "title": "Featured Items", "limit": 4 } },
     { "componentType": "MarkdownText", "props": { "content": "About us..." } }
   ]
   ```

### 2. Frontend Dynamic Renderer
1. Create `frontend/src/features/storefront/StorefrontRenderer.tsx`.
2. Fetch the schema via `GET /api/v1/storefront` using React Query.
3. Implement a component mapper that maps `componentType` strings to actual React components:
   - `HeroBanner` -> Renders a Tailwind CSS hero section.
   - `ProductGrid` -> Fetches products and renders a grid.
   - `MarkdownText` -> Renders markdown using `react-markdown`.
4. The renderer must dynamically pass `props` from the JSON schema to the React components.
5. If an unknown `componentType` is encountered, render a graceful fallback placeholder.

### 3. Storefront Editor (Optional but Recommended)
1. Create `frontend/src/features/storefront/StorefrontEditor.tsx`.
2. Provide a simple UI to add new components to the JSON schema and reorder them (drag-and-drop or up/down arrows).
3. On save, call `PUT /api/v1/storefront` with the updated JSON array.

### 4. Verification (Output Instructions)
- Provide instructions to manually test: Update the backend JSON schema to reorder components -> Refresh the frontend -> Verify the UI layout changes dynamically without frontend code changes.

## Target File Paths
- `backend/src/main/java/com/vantage/storefront/domain/StorefrontConfig.java`
- `backend/src/main/java/com/vantage/storefront/domain/StorefrontRepository.java`
- `backend/src/main/java/com/vantage/storefront/ui/StorefrontController.java`
- `backend/src/main/java/com/vantage/storefront/ui/dto/StorefrontLayoutResponse.java`
- `frontend/src/features/storefront/StorefrontRenderer.tsx`
- `frontend/src/features/storefront/StorefrontEditor.tsx`
- `frontend/src/features/storefront/components/HeroBanner.tsx`
- `frontend/src/features/storefront/components/ProductGrid.tsx`
- `frontend/src/features/storefront/components/MarkdownText.tsx`
