import { BrowserRouter, Route, Routes } from "react-router-dom";
import { ForecastDashboard } from "./features/analytics/ForecastDashboard";

function App() {
	return (
		<BrowserRouter>
			<div className="container mx-auto p-4">
				<Routes>
					<Route path="/" element={<div className="text-2xl font-bold text-gray-800">Vantage Dashboard</div>} />
					<Route path="/forecast" element={<ForecastDashboard />} />
				</Routes>
			</div>
		</BrowserRouter>
	);
}

export default App;
