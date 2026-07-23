# TASK-034: Implement Event-Driven Cache Invalidation (Caffeine)

## Product Context
- Read: `docs/00-product/01-vision-and-personas.md` (Understand performance expectations)
- Read: `docs/01-architecture/01-system-design-and-architecture.md` (Section 5.1 Module Responsibilities)

## Objective
Implement a high-performance, in-memory caching layer using Caffeine to cache the Product Catalog and the AI Forecast results. To prevent serving stale data, cache invalidation must be event-driven. When a product is updated or an order is placed, the system must publish an internal event that triggers the eviction of the corresponding cache entry, demonstrating advanced performance optimization and event-driven architecture patterns.

## Execution Boundaries
- You may ONLY create or modify files inside `backend/src/main/java/com/vantage/core/cache/`, `backend/src/main/java/com/vantage/product/`, `backend/src/main/java/com/vantage/analytics/`, `backend/src/main/resources/application.yml`, and their corresponding test directories.
- DO NOT modify `build.gradle.kts` (assume Caffeine and Spring Cache are already available via spring-boot-starter-cache).

## Context Files to Inject
- `docs/03-meta/agent-protocol.md`
- `backend/src/main/java/com/vantage/product/domain/Product.java`
- `backend/src/main/java/com/vantage/product/app/ProductService.java`
- `backend/src/main/java/com/vantage/analytics/app/AnalyticsService.java`

## Acceptance Criteria

### 1. Cache Configuration
1. Create `CacheConfig` in `com.vantage.core.cache` defining Caffeine cache beans.
2. Configure two caches:
   - `productCache`: maxSize 500, expireAfterWrite 1 hour.
   - `forecastCache`: maxSize 100, expireAfterWrite 10 minutes.
3. Enable caching via `@EnableCaching` on a configuration class.

### 2. Caching the Product Catalog
1. In `ProductService`, annotate the `getProductById` method with `@Cacheable(value = "productCache", key = "#id")`.
2. Annotate the `updateProduct` and `deleteProduct` methods (if they exist, or add them if missing) with `@CacheEvict(value = "productCache", key = "#product.id")`.

### 3. Caching the AI Forecast
1. In `AnalyticsService`, annotate the `getForecast` method with `@Cacheable(value = "forecastCache", key = "#productId")`.
2. Because the forecast depends on order history, we cannot simply rely on TTL. We need event-driven eviction.

### 4. Event-Driven Cache Eviction for Forecasts
1. Create `ForecastCacheEvictionListener` in `com.vantage.analytics.messaging`.
2. Listen to the internal Spring Application Event `OrderCreatedEvent` (published from the `order` module).
3. Note: To maintain Spring Modulith boundaries, the `analytics` module should listen to the event published by the `order` module.
4. When an `OrderCreatedEvent` is received, extract the `productId`.
5. Use the `CacheManager` to manually evict the `productId` from the `forecastCache`: `cacheManager.getCache("forecastCache").evict(productId);`.

### 5. Integration Testing (Testcontainers)
1. Create `CacheInvalidationIT` in `backend/src/test/java/com/vantage/core/cache/`.
2. Setup: Register vendor, create product.
3. Call `GET /api/v1/analytics/forecast/{productId}`. Verify it computes and caches the result.
4. Call it again. Verify the response time is significantly faster (cache hit).
5. Place an order for that product (triggers `OrderCreatedEvent`).
6. Call the forecast endpoint again. Verify the result reflects the new order (cache was evicted and recomputed).

## Target File Paths
- `backend/src/main/java/com/vantage/core/cache/CacheConfig.java`
- `backend/src/main/java/com/vantage/product/app/ProductService.java` (Modify)
- `backend/src/main/java/com/vantage/analytics/app/AnalyticsService.java` (Modify)
- `backend/src/main/java/com/vantage/analytics/messaging/ForecastCacheEvictionListener.java`
- `backend/src/test/java/com/vantage/core/cache/CacheInvalidationIT.java`
