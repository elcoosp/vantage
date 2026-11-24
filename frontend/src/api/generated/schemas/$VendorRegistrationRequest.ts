/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $VendorRegistrationRequest = {
    properties: {
        email: {
            type: 'string',
            isRequired: true,
            format: 'email',
        },
        password: {
            type: 'string',
            isRequired: true,
            minLength: 8,
        },
        name: {
            type: 'string',
            description: `Company / store name shown in the dashboard.`,
            isRequired: true,
        },
    },
} as const;
