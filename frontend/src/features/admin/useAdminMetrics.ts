import { useQuery } from "@tanstack/react-query";
import apiClient from "../../lib/api";

export interface SystemMetrics {
	totalVendors: number;
	totalOrders: number;
	paymentCircuitBreakerState: string;
}

async function fetchMetrics(): Promise<SystemMetrics> {
	const response = await apiClient.get<SystemMetrics>("/admin/metrics");
	return response.data;
}

export function useAdminMetrics() {
	return useQuery({
		queryKey: ["adminMetrics"],
		queryFn: fetchMetrics,
		refetchInterval: 5000,
		staleTime: 4000,
		retry: false,
	});
}
