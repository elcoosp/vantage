import { Link, Route, Routes } from "react-router-dom";
import { OrdersPage } from "./features/orders/OrdersPage";

function App() {
	return (
		<div className="min-h-screen bg-slate-900 text-slate-100">
			<nav className="bg-slate-800 border-b border-slate-700 px-6 py-4">
				<div className="container mx-auto flex justify-between items-center">
					<h1 className="text-xl font-bold text-slate-100">Vantage Dashboard</h1>
					<div className="space-x-4">
						<Link to="/" className="text-slate-300 hover:text-white transition-colors">
							Home
						</Link>
						<Link to="/orders" className="text-slate-300 hover:text-white transition-colors">
							Orders
						</Link>
					</div>
				</div>
			</nav>
			<main className="container mx-auto p-6">
				<Routes>
					<Route path="/" element={<div className="text-slate-300">Welcome to Vantage.</div>} />
					<Route path="/orders" element={<OrdersPage />} />
				</Routes>
			</main>
		</div>
	);
}

export default App;
