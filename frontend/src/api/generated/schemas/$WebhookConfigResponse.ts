/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $WebhookConfigResponse = {
    properties: {
        webhookUrl: {
            type: 'string',
            description: `Registered webhook URL, empty string when unset.`,
        },
        secret: {
            type: 'string',
            description: `Masked signing secret (e.g. abcd…wxyz), null when unset.`,
            isNullable: true,
        },
    },
} as const;
