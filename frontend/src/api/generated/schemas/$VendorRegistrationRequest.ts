/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $VendorRegistrationRequest = {
	properties: {
		email: {
			type: "string",
			isRequired: true,
			format: "email",
		},
		password: {
			type: "string",
			isRequired: true,
		},
		storeName: {
			type: "string",
			isRequired: true,
		},
		tenantSlug: {
			type: "string",
			isRequired: true,
		},
	},
} as const;
