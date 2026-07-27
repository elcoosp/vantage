/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $OrderRequest = {
	properties: {
		productId: {
			type: "string",
			isRequired: true,
			format: "uuid",
		},
		quantity: {
			type: "number",
			isRequired: true,
		},
	},
} as const;
