import { format, parseISO } from "date-fns";
import { Area, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
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
	if (!active || !payload || payload.length === 0) return null;
	const data = payload[0].payload;
	return (
		<div className="rounded-lg border border-line-light bg-card-light px-3 py-2 shadow-pop dark:border-line-dark dark:bg-card-dark">
			<p className="text-[12px] font-semibold text-ink-light dark:text-ink-dark">{data.formattedDate}</p>
			<div className="mt-1 space-y-1 text-[12px]">
				<p className="flex items-center gap-2">
					<span className="size-1.5 rounded-full bg-brand-500" />
					<span className="text-ink2-light dark:text-ink2-dark">Predicted</span>
					<span className="ml-auto pl-3 tabular-nums font-medium text-ink-light dark:text-ink-dark">
						{data.predictedQuantity}
					</span>
				</p>
				<p className="flex items-center gap-2">
					<span className="size-1.5 rounded-full bg-brand-300" />
					<span className="text-ink2-light dark:text-ink2-dark">Range</span>
					<span className="ml-auto pl-3 tabular-nums text-ink3-light dark:text-ink3-dark">
						{data.lowerBound}–{data.upperBound}
					</span>
				</p>
			</div>
		</div>
	);
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
		<div className="text-ink3-light dark:text-ink3-dark">
			<ResponsiveContainer width="100%" height={380}>
				<ComposedChart data={chartData} margin={{ top: 12, right: 12, left: 4, bottom: 4 }}>
					<CartesianGrid vertical={false} stroke="currentColor" strokeOpacity={0.08} />
					<XAxis
						dataKey="formattedDate"
						tick={{ fill: "currentColor", fontSize: 11 }}
						tickLine={false}
						axisLine={{ stroke: "currentColor", strokeOpacity: 0.2 }}
						minTickGap={20}
					/>
					<YAxis
						tick={{ fill: "currentColor", fontSize: 11 }}
						tickLine={false}
						axisLine={false}
						width={32}
						allowDecimals={false}
					/>
					<Tooltip content={<CustomTooltip />} />
					<Legend
						iconType="circle"
						iconSize={8}
						wrapperStyle={{ fontSize: 12, paddingTop: 8 }}
						formatter={(value) => <span className="text-ink2-light dark:text-ink2-dark">{value}</span>}
					/>
					<Area
						type="monotone"
						dataKey="upperBound"
						stroke="none"
						fill="#8389EA"
						fillOpacity={0.16}
						name="Confidence interval"
					/>
					<Area type="monotone" dataKey="lowerBound" stroke="none" fill="#F7F8FA" fillOpacity={1} name="Lower bound" />
					<Line
						type="monotone"
						dataKey="predictedQuantity"
						stroke="#5257CE"
						strokeWidth={2}
						strokeDasharray="5 5"
						name="Predicted quantity"
						dot={false}
						activeDot={{ r: 4, fillOpacity: 1 }}
					/>
				</ComposedChart>
			</ResponsiveContainer>
		</div>
	);
}
