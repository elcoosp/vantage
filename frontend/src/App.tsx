import { Toaster } from "react-hot-toast";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { CommandPalette } from "./components/CommandPalette";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AdminDashboard } from "./features/admin/AdminDashboard";
import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { InventoryGrid } from "./features/inventory/InventoryGrid";
import { OpsDashboard } from "./features/ops/OpsDashboard";
import { OrdersPage } from "./features/orders/OrdersPage";

function Products() {
	return (
		<div className="container mx-auto p-4">
			<h1 className="text-2xl font-bold text-gray-800">Products</h1>
		</div>
	);
}

function App() {
	return (
		<BrowserRouter>
			<Toaster position="top-right" />
			<CommandPalette />
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/register" element={<RegisterPage />} />
				<Route
					path="/"
					element={
						<ProtectedRoute>
							<Layout />
						</ProtectedRoute>
					}
				>
					<Route index element={<AdminDashboard />} />
					<Route path="inventory" element={<InventoryGrid />} />
					<Route path="ops" element={<OpsDashboard />} />
					<Route path="products" element={<Products />} />
					<Route path="orders" element={<OrdersPage />} />
				</Route>
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</BrowserRouter>
	);
}

export default App;
