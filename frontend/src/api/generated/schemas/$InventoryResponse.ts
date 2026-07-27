/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InventoryResponse = {
    properties: {
        productId: {
            type: 'string',
            format: 'uuid',
        },
        quantity: {
            type: 'number',
        },
        version: {
            type: 'number',
            description: `The new version number to be used in future If-Match headers.`,
        },
    },
} as const;
