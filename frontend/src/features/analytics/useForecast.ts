import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ForecastDataPoint {
	date: string;
	predictedQuantity: number;
	lowerBound: number;
	upperBound: number;
}

export interface ForecastResponse {
	forecast: ForecastDataPoint[];
}

async function fetchForecast(productId: string): Promise<ForecastResponse> {
	const response = await axios.get<ForecastResponse>(`/api/v1/analytics/forecast/${productId}`);
	return response.data;
}

export function useForecast(productId: string | null) {
	return useQuery({
		queryKey: ["forecast", productId],
		queryFn: () => {
			if (!productId) throw new Error("Product ID is required");
			return fetchForecast(productId);
		},
		enabled: !!productId,
		staleTime: 60000,
	});
}
