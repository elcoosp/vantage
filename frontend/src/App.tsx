import { Toaster } from "react-hot-toast";
import { Route, Routes } from "react-router-dom";
import { CommandPalette } from "./components/CommandPalette";
import { Layout } from "./components/Layout";
import { InventoryGrid } from "./features/inventory/InventoryGrid";

function Dashboard() {
	return (
		<div className="container mx-auto p-4">
			<h1 className="text-2xl font-bold text-gray-800">Dashboard</h1>
		</div>
	);
}

function Products() {
	return (
		<div className="container mx-auto p-4">
			<h1 className="text-2xl font-bold text-gray-800">Products</h1>
		</div>
	);
}

function App() {
	return (
		<>
			<Toaster position="top-right" />
			<CommandPalette />
			<Routes>
				<Route path="/" element={<Layout />}>
					<Route index element={<Dashboard />} />
					<Route path="inventory" element={<InventoryGrid />} />
					<Route path="products" element={<Products />} />
				</Route>
			</Routes>
		</>
	);
}

export default App;
