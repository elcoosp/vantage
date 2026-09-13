import { useQuery } from "@tanstack/react-query";
import { Loader2, TrendingUp } from "lucide-react";
import { useState } from "react";
import { Card, CardHeader, EmptyState, PageHeader, Select } from "../../components/ui";
import apiClient from "../../lib/api";
import { ForecastChart } from "./ForecastChart";
import { useForecast } from "./useForecast";

interface ProductSummary {
	id: string;
	name: string;
}

async function fetchProducts(): Promise<ProductSummary[]> {
	const res = await apiClient.get<ProductSummary[]>("/products");
	return res.data;
}

export function ForecastDashboard() {
	const [selectedProductId, setSelectedProductId] = useState<string | null>(null);
	const { data: products, isLoading: productsLoading } = useQuery({
		queryKey: ["forecast-products"],
		queryFn: fetchProducts,
		retry: false,
		staleTime: 60000,
	});
	const { data, isLoading, error } = useForecast(selectedProductId);

	const handleProductChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
		setSelectedProductId(event.target.value || null);
	};

	return (
		<div className="space-y-6 animate-fade-up">
			<PageHeader
				title="Demand forecast"
				description="AI-driven demand prediction (Holt-Winters with confidence intervals) for a selected product."
			/>

			<Card>
				<CardHeader title="Product" subtitle="Pick an item to project its next 30 days of demand." />
				<div className="px-5 pb-5">
					<Select
						id="product-select"
						value={selectedProductId || ""}
						onChange={handleProductChange}
						disabled={productsLoading}
						className="max-w-sm"
					>
						<option value="">Choose a product…</option>
						{products?.map((product) => (
							<option key={product.id} value={product.id}>
								{product.name}
							</option>
						))}
					</Select>
				</div>
			</Card>

			{selectedProductId && (
				<Card>
					<CardHeader title="Forecast" subtitle="Predicted daily demand with the expected range around it." />
					<div className="px-3 pb-4 text-ink3-light dark:text-ink3-dark">
						{isLoading && (
							<div className="flex h-72 items-center justify-center">
								<Loader2 className="size-6 animate-spin text-brand-600 dark:text-brand-400" />
							</div>
						)}

						{error && (
							<div className="m-5 rounded-lg border border-status-danger-soft-light bg-status-danger-soft-light px-4 py-3 text-sm text-status-danger-ink-light dark:border-[#E5484D]/25 dark:bg-[#E5484D]/10 dark:text-[#FF9A9D]">
								Failed to load the forecast. Please try again later.
							</div>
						)}

						{data && data.forecast.length > 0 && <ForecastChart data={data.forecast} />}

						{data && data.forecast.length === 0 && (
							<EmptyState
								icon={<TrendingUp className="size-5" />}
								title="No forecast available"
								description="There isn't enough order history for this product to project demand yet."
							/>
						)}
					</div>
				</Card>
			)}
		</div>
	);
}
