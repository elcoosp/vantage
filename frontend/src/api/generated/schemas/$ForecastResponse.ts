/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ForecastResponse = {
    properties: {
        productId: {
            type: 'string',
            format: 'uuid',
        },
        forecast: {
            type: 'array',
            contains: {
                properties: {
                    date: {
                        type: 'string',
                        format: 'date',
                    },
                    predictedQuantity: {
                        type: 'number',
                    },
                    lowerBound: {
                        type: 'number',
                    },
                    upperBound: {
                        type: 'number',
                    },
                },
            },
        },
    },
} as const;
