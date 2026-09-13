import {
	ArrowUpRight,
	Boxes,
	Code2,
	LayoutGrid,
	LogOut,
	Package,
	Search,
	ShoppingCart,
	TrendingUp,
} from "lucide-react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import { ChatWidget } from "../features/chat/ChatWidget";
import { useAuthStore } from "../store/authStore";
import { Kbd } from "./ui";

const NAV_SECTIONS: Array<{ label: string; items: Array<{ to: string; label: string; icon: typeof LayoutGrid }> }> = [
	{
		label: "Operations",
		items: [
			{ to: "/", label: "Overview", icon: LayoutGrid },
			{ to: "/orders", label: "Orders", icon: ShoppingCart },
			{ to: "/products", label: "Products", icon: Package },
			{ to: "/inventory", label: "Inventory", icon: Boxes },
		],
	},
	{
		label: "Insights",
		items: [
			{ to: "/forecast", label: "Analytics", icon: TrendingUp },
			{ to: "/developer", label: "Developer", icon: Code2 },
		],
	},
];

const ROUTE_TITLES: Record<string, string> = {
	"/": "Overview",
	"/orders": "Orders",
	"/products": "Products",
	"/inventory": "Inventory",
	"/forecast": "Demand forecast",
	"/developer": "Developer",
	"/ops": "Live map",
};

function BrandMark() {
	return (
		<span className="flex items-center gap-2.5">
			<img src={logo} alt="Vantage" className="size-7 shrink-0" />
			<span className="text-[15px] font-semibold tracking-tight text-ink-light dark:text-ink-dark">Vantage</span>
		</span>
	);
}

export function Layout() {
	const navigate = useNavigate();
	const location = useLocation();
	const accessToken = useAuthStore((state) => state.accessToken);
	const tenantId = useAuthStore((state) => state.tenantId);
	const clearAuth = useAuthStore((state) => state.clearAuth);

	const handleLogout = () => {
		clearAuth();
		localStorage.removeItem("accessToken");
		localStorage.removeItem("tenantId");
		navigate("/login", { replace: true });
	};

	const pageTitle = ROUTE_TITLES[location.pathname] ?? "Overview";

	return (
		<>
			<div className="flex h-screen overflow-hidden bg-canvas-light text-ink-light dark:bg-canvas-dark dark:text-ink-dark">
				{/* Sidebar */}
				<aside className="flex w-60 shrink-0 flex-col border-r border-line-light bg-card-light dark:border-line-dark dark:bg-card-dark">
					<div className="flex h-16 items-center px-5">
						<Link to="/" aria-label="Vantage home">
							<BrandMark />
						</Link>
					</div>

					<nav className="flex-1 overflow-y-auto px-3 pb-4 custom-scrollbar" aria-label="Primary">
						{NAV_SECTIONS.map((section) => (
							<div key={section.label} className="mt-5 first:mt-2">
								<p className="px-2 pb-2 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink3-light dark:text-ink3-dark">
									{section.label}
								</p>
								<ul className="space-y-0.5">
									{section.items.map((item) => (
										<li key={item.to}>
											<NavLink
												to={item.to}
												end={item.to === "/"}
												className={({ isActive }) =>
													`group flex h-9 items-center gap-2.5 rounded-lg px-2.5 text-[13.5px] font-medium transition-colors duration-150 ${
														isActive
															? "bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-100"
															: "text-ink2-light hover:bg-card2-light hover:text-ink-light dark:text-ink2-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark"
													}`
												}
											>
												{({ isActive }) => (
													<>
														<item.icon
															className={`size-4 shrink-0 transition-colors ${
																isActive
																	? "text-brand-600 dark:text-brand-300"
																	: "text-ink3-light group-hover:text-ink2-light dark:text-ink3-dark dark:group-hover:text-ink2-dark"
															}`}
														/>
														<span className="truncate">{item.label}</span>
														{isActive && (
															<span className="ml-auto size-1.5 shrink-0 rounded-full bg-brand-500 dark:bg-brand-400" />
														)}
													</>
												)}
											</NavLink>
										</li>
									))}
								</ul>
							</div>
						))}
					</nav>

					<div className="border-t border-line-light p-3 dark:border-line-dark">
						<div className="mb-2 flex items-center gap-2 px-2 py-1.5">
							<span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-card2-light text-[10px] font-semibold text-ink2-light dark:bg-card2-dark dark:text-ink2-dark">
								{(tenantId ?? "T").slice(0, 2).toUpperCase()}
							</span>
							<span className="min-w-0 flex-1 text-xs">
								<span className="block truncate font-medium text-ink-light dark:text-ink-dark">
									Tenant {tenantId?.slice(0, 8)}
								</span>
								<span className="block truncate text-ink3-light dark:text-ink3-dark">Vantage</span>
							</span>
						</div>
						<button
							type="button"
							onClick={handleLogout}
							className="flex h-8 w-full items-center gap-2 rounded-lg px-2.5 text-[13px] font-medium text-ink3-light transition-colors hover:bg-card2-light hover:text-status-danger-ink-light dark:text-ink3-dark dark:hover:bg-card2-dark dark:hover:text-[#FF9A9D]"
						>
							<LogOut className="size-3.5" />
							Sign out
						</button>
					</div>
				</aside>

				{/* Main column */}
				<div className="flex min-w-0 flex-1 flex-col">
					<header className="flex h-16 shrink-0 items-center justify-between border-b border-line-light bg-canvas-light/80 px-6 backdrop-blur dark:border-line-dark dark:bg-canvas-dark/80">
						<div className="min-w-0">
							<p className="text-[13px] font-medium tracking-tight text-ink-light dark:text-ink-dark">{pageTitle}</p>
						</div>
						<div className="flex items-center gap-3">
							<button
								type="button"
								onClick={() => document.dispatchEvent(new KeyboardEvent("keydown", { key: "k", metaKey: true }))}
								className="flex h-8 items-center gap-2 rounded-lg border border-line-light bg-card-light px-2.5 text-[13px] font-medium text-ink3-light transition-colors hover:border-line2-light hover:text-ink2-light dark:border-line-dark dark:bg-card-dark dark:text-ink3-dark dark:hover:border-line2-dark dark:hover:text-ink2-dark"
							>
								<Search className="size-3.5" />
								<span className="hidden sm:inline">Search</span>
								<Kbd>⌘K</Kbd>
							</button>
							{accessToken && (
								<a
									href="/ops"
									className="hidden items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[13px] font-medium text-ink3-light transition-colors hover:bg-card2-light hover:text-ink-light md:flex dark:text-ink3-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark"
								>
									Live map
									<ArrowUpRight className="size-3.5" />
								</a>
							)}
						</div>
					</header>

					<main className="flex-1 overflow-y-auto custom-scrollbar">
						<div className="mx-auto max-w-[1200px] px-6 py-6 lg:px-8 lg:py-8">
							<Outlet />
						</div>
					</main>
				</div>
			</div>
			<ChatWidget />
		</>
	);
}
