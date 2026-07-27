/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PaymentRequest = {
	properties: {
		orderId: {
			type: "string",
			isRequired: true,
			format: "uuid",
		},
		amount: {
			type: "number",
			isRequired: true,
		},
		currency: {
			type: "string",
		},
	},
} as const;
