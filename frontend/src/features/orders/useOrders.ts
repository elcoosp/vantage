import { useQuery } from "@tanstack/react-query";
import apiClient from "../../lib/api";

export interface Order {
	orderId: string;
	productName: string;
	status: string;
	quantity: number;
	createdAt: string;
}

export interface OrderSearchResponse {
	content: Order[];
	totalPages: number;
	totalElements: number;
	number: number;
	size: number;
}

async function fetchOrders(): Promise<OrderSearchResponse> {
	const response = await apiClient.get<OrderSearchResponse>("/orders/search", {
		params: {
			size: 10000,
		},
	});
	return response.data;
}

export function useOrders() {
	return useQuery({
		queryKey: ["orders", 10000],
		queryFn: fetchOrders,
		staleTime: 1000 * 60 * 5,
		retry: false,
	});
}
