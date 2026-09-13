import { Toaster } from "react-hot-toast";
import { Route, Routes } from "react-router-dom";
import { CommandPalette } from "./components/CommandPalette";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { ForecastDashboard } from "./features/analytics/ForecastDashboard";
import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { AdminDashboard } from "./features/admin/AdminDashboard";
import { DeveloperPortal } from "./features/developer/DeveloperPortal";
import { InventoryGrid } from "./features/inventory/InventoryGrid";
import { OpsDashboard } from "./features/ops/OpsDashboard";
import { OrdersPage } from "./features/orders/OrdersPage";

import { Dashboard } from "./features/dashboard/Dashboard";
import { Products } from "./features/products/Products";

function App() {
	return (
		<>
			<Toaster
				position="top-right"
				toastOptions={{
					className:
						"!rounded-lg !border !border-line-light !bg-card-light !px-3.5 !py-2.5 !text-[13px] !text-ink-light !shadow-pop dark:!border-line-dark dark:!bg-card-dark dark:!text-ink-dark",
				}}
			/>
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
					<Route index element={<Dashboard />} />
					<Route path="inventory" element={<InventoryGrid />} />
					<Route path="ops" element={<OpsDashboard />} />
					<Route path="products" element={<Products />} />
					<Route path="orders" element={<OrdersPage />} />
					<Route path="forecast" element={<ForecastDashboard />} />
					<Route path="developer" element={<DeveloperPortal />} />
					<Route path="admin" element={<AdminDashboard />} />
				</Route>
			</Routes>
		</>
	);
}

export default App;
