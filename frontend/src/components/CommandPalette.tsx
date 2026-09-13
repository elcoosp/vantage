import {
	Activity,
	AppWindow,
	BarChart3,
	Boxes,
	Code2,
	CreditCard,
	LayoutDashboard,
	Map as MapIcon,
	Search,
	Settings,
	Store,
	Users,
	Webhook,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUIStore } from "../store/uiStore";
import { Kbd } from "./ui";

interface CommandItem {
	id: string;
	label: string;
	description?: string;
	icon: typeof Search;
	action: () => void;
}

export function CommandPalette() {
	const [open, setOpen] = useState(false);
	const [query, setQuery] = useState("");
	const navigate = useNavigate();
	const { openAddProductModal, openUpdateStockModal } = useUIStore();

	useEffect(() => {
		const down = (e: KeyboardEvent) => {
			if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
				e.preventDefault();
				setOpen((prev) => !prev);
			}
			if (e.key === "Escape") {
				setOpen(false);
			}
		};
		document.addEventListener("keydown", down);
		return () => document.removeEventListener("keydown", down);
	}, []);

	useEffect(() => {
		if (open) {
			setQuery("");
			requestAnimationFrame(() => document.getElementById("command-palette-input")?.focus());
		}
	}, [open]);

	if (!open) return null;

	const go = (path: string) => {
		navigate(path);
		setOpen(false);
	};

	const run = (action: () => void) => {
		setOpen(false);
		action();
	};

	const items: CommandItem[] = [
		{ id: "dashboard", label: "Dashboard", icon: LayoutDashboard, action: () => go("/") },
		{ id: "orders", label: "Orders", icon: CreditCard, action: () => go("/orders") },
		{ id: "products", label: "Products", icon: Store, action: () => go("/products") },
		{ id: "inventory", label: "Inventory", icon: Boxes, action: () => go("/inventory") },
		{ id: "forecast", label: "Demand forecast", icon: BarChart3, action: () => go("/forecast") },
		{ id: "developer", label: "Developer — API keys", icon: Code2, action: () => go("/developer") },
		{ id: "webhooks", label: "Developer — webhooks", icon: Webhook, action: () => go("/developer") },
		{ id: "api-logs", label: "Developer — live logs", icon: Activity, action: () => go("/developer") },
		{ id: "ops", label: "Operations command", icon: AppWindow, action: () => go("/ops") },
		{ id: "map", label: "Live order map", icon: MapIcon, action: () => go("/ops") },
		{ id: "admin", label: "Admin dashboard", icon: Settings, action: () => go("/admin") },
		{ id: "team", label: "Team & permissions", icon: Users, action: () => go("/team") },
		{ id: "add-product", label: "Add product", icon: Store, action: openAddProductModal },
		{ id: "update-stock", label: "Update stock", icon: Boxes, action: openUpdateStockModal },
	];

	const q = query.trim().toLowerCase();
	const results = q
		? items.filter(
				(item) => item.label.toLowerCase().includes(q) || (item.description?.toLowerCase().includes(q) ?? false),
			)
		: items;

	return (
		<div
			className="fixed inset-0 z-50 flex items-start justify-center bg-black/40 p-4 pt-[12vh] backdrop-blur-sm"
			onMouseDown={(e) => {
				if (e.target === e.currentTarget) setOpen(false);
			}}
		>
			<div className="w-full max-w-lg overflow-hidden rounded-xl border border-line-light bg-card-light shadow-float animate-scale-in dark:border-line-dark dark:bg-card-dark">
				<div className="flex items-center gap-3 border-b border-line-light px-4 dark:border-line-dark">
					<Search className="size-4 shrink-0 text-ink3-light dark:text-ink3-dark" />
					<input
						id="command-palette-input"
						value={query}
						onChange={(e) => setQuery(e.target.value)}
						placeholder="Search pages & actions…"
						className="h-12 w-full bg-transparent text-sm text-ink-light outline-none placeholder:text-ink3-light dark:text-ink-dark dark:placeholder:text-ink3-dark"
					/>
					<Kbd>esc</Kbd>
				</div>

				<div className="max-h-80 overflow-y-auto p-2 custom-scrollbar">
					{results.length === 0 && (
						<p className="px-3 py-6 text-center text-sm text-ink3-light dark:text-ink3-dark">
							No matches for “{query}”.
						</p>
					)}
					{results.map((item, index) => {
						const Icon = item.icon;
						return (
							<button
								type="button"
								key={item.id}
								onClick={() => run(item.action)}
								className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors ${
									index === 0 ? "bg-card2-light dark:bg-card2-dark" : "hover:bg-card2-light dark:hover:bg-card2-dark"
								}`}
							>
								<span
									className={`flex size-7 shrink-0 items-center justify-center rounded-md ${
										index === 0
											? "bg-brand-500/15 text-brand-600 dark:bg-brand-500/20 dark:text-brand-300"
											: "bg-card2-light text-ink3-light dark:bg-card2-dark dark:text-ink3-dark"
									}`}
								>
									<Icon className="size-4" />
								</span>
								<div className="min-w-0">
									<p className="truncate text-[13px] font-medium text-ink-light dark:text-ink-dark">{item.label}</p>
									{item.description && (
										<p className="truncate text-[12px] text-ink3-light dark:text-ink3-dark">{item.description}</p>
									)}
								</div>
							</button>
						);
					})}
				</div>
			</div>
		</div>
	);
}
