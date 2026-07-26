import { useState } from "react";
import { ForecastChart } from "./ForecastChart";
import { useForecast } from "./useForecast";

const MOCK_PRODUCTS = [
	{ id: "550e8400-e29b-41d4-a716-446655440000", name: "Product A" },
	{ id: "550e8400-e29b-41d4-a716-446655440001", name: "Product B" },
	{ id: "550e8400-e29b-41d4-a716-446655440002", name: "Product C" },
];

export function ForecastDashboard() {
	const [selectedProductId, setSelectedProductId] = useState<string | null>(null);
	const { data, isLoading, error } = useForecast(selectedProductId);

	const handleProductChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
		setSelectedProductId(event.target.value || null);
	};

	return (
		<div className="p-6 space-y-6">
			<h2 className="text-2xl font-bold text-gray-800">AI Demand Forecast</h2>

			<div className="bg-white shadow rounded-lg p-4">
				<label htmlFor="product-select" className="block text-sm font-medium text-gray-700 mb-2">
					Select Product
				</label>
				<select
					id="product-select"
					value={selectedProductId || ""}
					onChange={handleProductChange}
					className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
				>
					<option value="">-- Choose a product --</option>
					{MOCK_PRODUCTS.map((product) => (
						<option key={product.id} value={product.id}>
							{product.name}
						</option>
					))}
				</select>
			</div>

			{selectedProductId && (
				<div className="bg-white shadow rounded-lg p-4">
					{isLoading && (
						<div className="flex items-center justify-center h-64">
							<div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
						</div>
					)}

					{error && (
						<div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
							Failed to load forecast data. Please try again.
						</div>
					)}

					{data && data.forecast.length > 0 && <ForecastChart data={data.forecast} />}

					{data && data.forecast.length === 0 && (
						<div className="text-center text-gray-500 py-12">No forecast data available for this product.</div>
					)}
				</div>
			)}
		</div>
	);
}
