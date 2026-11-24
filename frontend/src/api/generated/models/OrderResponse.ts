/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type OrderResponse = {
    orderId?: string;
    /**
     * Present on responses from the non-generated controller (202 flow).
     */
    productId?: string;
    /**
     * Present on responses from the non-generated controller (202 flow).
     */
    quantity?: number;
    /**
     * CREATED orders are processed asynchronously by the RabbitMQ saga
     * (inventory reservation + payment) and typically reach PAID within
     * seconds. SHIPPED/DELIVERED are set by the fulfillment endpoints.
     *
     */
    status?: OrderResponse.status;
};
export namespace OrderResponse {
    /**
     * CREATED orders are processed asynchronously by the RabbitMQ saga
     * (inventory reservation + payment) and typically reach PAID within
     * seconds. SHIPPED/DELIVERED are set by the fulfillment endpoints.
     *
     */
    export enum status {
        CREATED = 'CREATED',
        CONFIRMED = 'CONFIRMED',
        PAID = 'PAID',
        SHIPPED = 'SHIPPED',
        DELIVERED = 'DELIVERED',
        CANCELLED = 'CANCELLED',
    }
}

