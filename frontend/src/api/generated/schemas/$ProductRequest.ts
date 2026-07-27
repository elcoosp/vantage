/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ProductRequest = {
	properties: {
		name: {
			type: "string",
			isRequired: true,
		},
		price: {
			type: "number",
			isRequired: true,
			format: "double",
		},
		sku: {
			type: "string",
		},
	},
} as const;
