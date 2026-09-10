import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { TrendingUp } from "lucide-react";
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
		<div className="p-6 space-y-6">
			<div className="flex items-center gap-3">
				<TrendingUp className="h-6 w-6 text-slate-600 dark:text-slate-300" />
				<h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
					AI Demand Forecast
				</h2>
			</div>

			<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-4">
				<label
					htmlFor="product-select"
					className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2"
				>
					Select Product
				</label>
				<select
					id="product-select"
					value={selectedProductId || ""}
					onChange={handleProductChange}
					className="block w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 bg-white dark:bg-slate-900 text-slate-900"
					disabled={productsLoading}
				>
					<option value="">-- Choose a product --</option>
					{products?.map((product) => (
						<option key={product.id} value={product.id}>
							{product.name}
						</option>
					))}
				</select>
			</div>

			{selectedProductId && (
				<div className="bg-white dark:bg-slate-800 rounded-xl shadow border border-slate-200 dark:border-slate-700 p-4">
					{isLoading && (
						<div className="flex items-center justify-center h-64">
							<div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
						</div>
					)}

					{error && (
						<div className="bg-red-900/20 border border-red-700 text-red-200 px-4 py-3 rounded">
							Failed to load forecast data. Please try again later.
						</div>
					)}

					{data && data.forecast.length > 0 && <ForecastChart data={data.forecast} />}

					{data && data.forecast.length === 0 && (
						<div className="text-center text-slate-500 dark:text-slate-400 py-12">
							No forecast data available for this product.
						</div>
					)}
				</div>
			)}
		</div>
	);
}
