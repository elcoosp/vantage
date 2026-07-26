import { format, parseISO } from "date-fns";
import { Area, ComposedChart, Legend, Line, Tooltip, XAxis, YAxis } from "recharts";
import type { ForecastDataPoint } from "./useForecast";

interface ForecastChartProps {
	data: ForecastDataPoint[];
}

interface ChartDataPoint {
	date: string;
	formattedDate: string;
	predictedQuantity: number;
	lowerBound: number;
	upperBound: number;
}

interface CustomTooltipProps {
	active?: boolean;
	payload?: Array<{ payload: ChartDataPoint }>;
	label?: string;
}

function CustomTooltip({ active, payload }: CustomTooltipProps) {
	if (active && payload && payload.length) {
		const data = payload[0].payload;
		return (
			<div className="bg-white p-3 border border-gray-200 rounded shadow">
				<p className="font-semibold">{data.formattedDate}</p>
				<p className="text-blue-600">Predicted: {data.predictedQuantity}</p>
				<p className="text-gray-500">
					Range: {data.lowerBound} - {data.upperBound}
				</p>
			</div>
		);
	}
	return null;
}

export function ForecastChart({ data }: ForecastChartProps) {
	const chartData: ChartDataPoint[] = data.map((point) => ({
		date: point.date,
		formattedDate: format(parseISO(point.date), "MMM dd"),
		predictedQuantity: point.predictedQuantity,
		lowerBound: point.lowerBound,
		upperBound: point.upperBound,
	}));

	return (
		<ComposedChart width={800} height={400} data={chartData}>
			<XAxis dataKey="formattedDate" />
			<YAxis />
			<Tooltip content={<CustomTooltip />} />
			<Legend />
			<Area
				type="monotone"
				dataKey="upperBound"
				stroke="none"
				fill="#93c5fd"
				fillOpacity={0.3}
				name="Confidence Interval"
			/>
			<Area type="monotone" dataKey="lowerBound" stroke="none" fill="#ffffff" fillOpacity={1} name="Lower Bound" />
			<Line
				type="monotone"
				dataKey="predictedQuantity"
				stroke="#2563eb"
				strokeWidth={2}
				strokeDasharray="5 5"
				name="Predicted Quantity"
				dot={{ fill: "#2563eb" }}
			/>
		</ComposedChart>
	);
}
