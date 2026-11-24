/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AuthResponse = {
    properties: {
        token: {
            type: 'string',
            description: `HS256 JWT (24h expiry) used as a Bearer token.`,
        },
        tenantId: {
            type: 'string',
            format: 'uuid',
        },
    },
} as const;
