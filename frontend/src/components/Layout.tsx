import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { ChatWidget } from "../features/chat/ChatWidget";

export function Layout() {
	const navigate = useNavigate();
	const accessToken = useAuthStore((state) => state.accessToken);
	const clearAuth = useAuthStore((state) => state.clearAuth);

	const handleLogout = () => {
		clearAuth();
		localStorage.removeItem("accessToken");
		localStorage.removeItem("tenantId");
		navigate("/login");
	};

	return (
		<>
			<div className="flex h-screen bg-gray-100 dark:bg-gray-900">
				<aside className="w-64 bg-gray-800 dark:bg-gray-950 text-white flex flex-col">
					<div className="p-4 text-xl font-bold border-b border-gray-700">Vantage</div>
					<nav className="flex-1 p-4 space-y-2">
						<Link to="/" className="block px-3 py-2 rounded hover:bg-gray-700">
							Dashboard
						</Link>
						<Link to="/inventory" className="block px-3 py-2 rounded hover:bg-gray-700">
							Inventory
						</Link>
						<Link to="/products" className="block px-3 py-2 rounded hover:bg-gray-700">
							Products
						</Link>
						<Link to="/ops" className="block px-3 py-2 rounded hover:bg-gray-700">
							Ops
						</Link>
					</nav>
					{accessToken && (
						<div className="p-4 border-t border-gray-700">
							<button
								type="button"
								onClick={handleLogout}
								className="w-full px-3 py-2 text-sm rounded bg-red-600 hover:bg-red-700"
							>
								Sign Out
							</button>
						</div>
					)}
				</aside>
				<div className="flex-1 flex flex-col overflow-hidden">
					<header className="h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center px-6">
						<h1 className="text-xl font-semibold">Dashboard</h1>
					</header>
					<main className="flex-1 overflow-y-auto p-6">
						<Outlet />
					</main>
				</div>
			</div>
			<ChatWidget />
		</>
	);
}
