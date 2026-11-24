/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $OrderResponse = {
    properties: {
        orderId: {
            type: 'string',
            format: 'uuid',
        },
        productId: {
            type: 'string',
            description: `Present on responses from the non-generated controller (202 flow).`,
            format: 'uuid',
        },
        quantity: {
            type: 'number',
            description: `Present on responses from the non-generated controller (202 flow).`,
        },
        status: {
            type: 'Enum',
        },
    },
} as const;
