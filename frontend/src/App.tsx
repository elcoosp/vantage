import { Route, Routes } from "react-router-dom";
import { OpsDashboard } from "./features/ops/OpsDashboard";

function App() {
	return (
		<div className="container mx-auto p-4">
			<Routes>
				<Route path="/" element={<h1 className="text-2xl font-bold text-gray-800">Vantage Dashboard</h1>} />
				<Route path="/ops" element={<OpsDashboard />} />
			</Routes>
		</div>
	);
}

export default App;
