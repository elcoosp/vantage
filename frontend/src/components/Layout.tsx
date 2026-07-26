import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

interface LayoutProps {
	children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
	const navigate = useNavigate();
	const clearAuth = useAuthStore((state) => state.clearAuth);

	const handleLogout = () => {
		clearAuth();
		localStorage.removeItem("accessToken");
		localStorage.removeItem("tenantId");
		navigate("/login");
	};

	return (
		<div className="min-h-screen bg-gray-100">
			<nav className="bg-white shadow-sm">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<div className="flex justify-between h-16">
						<div className="flex items-center">
							<span className="text-xl font-semibold text-gray-800">Vantage</span>
						</div>
						<div className="flex items-center">
							<button
								type="button"
								onClick={handleLogout}
								className="text-sm font-medium text-gray-700 hover:text-gray-900"
							>
								Logout
							</button>
						</div>
					</div>
				</div>
			</nav>
			<main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">{children}</main>
		</div>
	);
}
