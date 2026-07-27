/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type PaymentResponse = {
	transactionId?: string;
	status?: PaymentResponse.status;
};
export namespace PaymentResponse {
	export enum status {
		SUCCESS = "SUCCESS",
		FAILED = "FAILED",
	}
}
