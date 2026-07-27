/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ForecastResponse = {
	productId?: string;
	forecast?: Array<{
		date?: string;
		predictedQuantity?: number;
		lowerBound?: number;
		upperBound?: number;
	}>;
};
