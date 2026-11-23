/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type WebhookConfigResponse = {
    /**
     * Registered webhook URL, empty string when unset.
     */
    webhookUrl?: string;
    /**
     * Masked signing secret (e.g. abcd…wxyz), null when unset.
     */
    secret?: string | null;
};

