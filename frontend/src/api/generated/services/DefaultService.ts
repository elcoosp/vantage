/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AuthResponse } from '../models/AuthResponse';
import type { ForecastResponse } from '../models/ForecastResponse';
import type { InventoryResponse } from '../models/InventoryResponse';
import type { InventoryUpdateRequest } from '../models/InventoryUpdateRequest';
import type { OrderRequest } from '../models/OrderRequest';
import type { OrderResponse } from '../models/OrderResponse';
import type { PaymentRequest } from '../models/PaymentRequest';
import type { PaymentResponse } from '../models/PaymentResponse';
import type { ProductRequest } from '../models/ProductRequest';
import type { ProductResponse } from '../models/ProductResponse';
import type { VendorRegistrationRequest } from '../models/VendorRegistrationRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DefaultService {
    /**
     * Register a new Vendor (Tenant)
     * @param requestBody
     * @returns AuthResponse Vendor created successfully
     * @throws ApiError
     */
    public static postApiV1VendorsRegister(
        requestBody: VendorRegistrationRequest,
    ): CancelablePromise<AuthResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/vendors/register',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `Tenant slug already exists`,
            },
        });
    }
    /**
     * Create a Product
     * @param requestBody
     * @returns ProductResponse Product created
     * @throws ApiError
     */
    public static postApiV1Products(
        requestBody: ProductRequest,
    ): CancelablePromise<ProductResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/products',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Update inventory quantity (Optimistic Locking)
     * Uses the `If-Match` header with the current entity version.
     * If the version does not match, returns 409 Conflict.
     *
     * @param productId
     * @param ifMatch The current version of the inventory entity.
     * @param requestBody
     * @returns InventoryResponse Inventory updated
     * @throws ApiError
     */
    public static putApiV1Inventory(
        productId: string,
        ifMatch: number,
        requestBody: InventoryUpdateRequest,
    ): CancelablePromise<InventoryResponse> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/v1/inventory/{productId}',
            path: {
                'productId': productId,
            },
            headers: {
                'If-Match': ifMatch,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `Conflict - Inventory was modified by another transaction.`,
            },
        });
    }
    /**
     * Place an Order (Triggers Saga)
     * Initiates the distributed transaction. Returns 202 Accepted immediately.
     * @param requestBody
     * @returns OrderResponse Order accepted and processing
     * @throws ApiError
     */
    public static postApiV1Orders(
        requestBody: OrderRequest,
    ): CancelablePromise<OrderResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/orders',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * Process Payment (Idempotent)
     * Requires an Idempotency-Key header. If a duplicate key is sent within 24h,
     * the original cached response is returned without reprocessing the payment.
     *
     * @param idempotencyKey Unique client-generated UUID for exactly-once payment processing.
     * @param requestBody
     * @returns PaymentResponse Payment processed
     * @throws ApiError
     */
    public static postApiV1Payments(
        idempotencyKey: string,
        requestBody: PaymentRequest,
    ): CancelablePromise<PaymentResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/payments',
            headers: {
                'Idempotency-Key': idempotencyKey,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                429: `Rate limit exceeded`,
            },
        });
    }
    /**
     * Get 7-day Demand Forecast
     * Uses pure Java Holt-Winters Exponential Smoothing.
     * @param productId
     * @returns ForecastResponse Forecast generated
     * @throws ApiError
     */
    public static getApiV1AnalyticsForecast(
        productId: string,
    ): CancelablePromise<ForecastResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/analytics/forecast/{productId}',
            path: {
                'productId': productId,
            },
        });
    }
}
