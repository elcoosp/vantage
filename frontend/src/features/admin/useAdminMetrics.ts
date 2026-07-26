import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface SystemMetrics {
	totalVendors: number;
	totalOrders: number;
	paymentCircuitBreakerState: string;
}

async function fetchMetrics(): Promise<SystemMetrics> {
	const response = await axios.get<SystemMetrics>("/api/v1/admin/metrics");
	return response.data;
}

export function useAdminMetrics() {
	return useQuery({
		queryKey: ["adminMetrics"],
		queryFn: fetchMetrics,
		refetchInterval: 5000,
		staleTime: 4000,
	});
}
