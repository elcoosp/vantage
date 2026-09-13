import { ArrowRight, Boxes, LineChart, ShieldCheck, ShoppingCart } from "lucide-react";
import type { ReactNode } from "react";
import logo from "../assets/logo.png";

interface AuthLayoutProps {
	children: ReactNode;
	title: string;
	subtitle?: string;
}

const WORTH = [
	{ icon: Boxes, text: "Products, inventory, and orders in one operating view" },
	{ icon: LineChart, text: "AI demand forecasting with confidence intervals" },
	{ icon: ShieldCheck, text: "Multi-tenant isolation with optimistic concurrency" },
];

function BrandMark({ dark = false }: { dark?: boolean }) {
	return (
		<span className="flex items-center gap-2.5">
			<img src={logo} alt="Vantage" className="size-7 shrink-0" />
			<span
				className={`text-[15px] font-semibold tracking-tight ${dark ? "text-white" : "text-ink-light dark:text-ink-dark"}`}
			>
				Vantage
			</span>
		</span>
	);
}

export function AuthLayout({ children, title, subtitle }: AuthLayoutProps) {
	return (
		<div className="min-h-screen bg-canvas-light lg:grid lg:grid-cols-[1.05fr_1fr] dark:bg-canvas-dark">
			{/* Brand panel */}
			<div className="relative hidden overflow-hidden bg-ink-light lg:flex lg:flex-col lg:justify-between lg:p-12 dark:bg-[#0A0B10]">
				<div
					className="pointer-events-none absolute inset-0 opacity-[0.07]"
					style={{
						backgroundImage: "radial-gradient(circle at 1px 1px, rgb(255 255 255 / 0.9) 1px, transparent 0)",
						backgroundSize: "26px 26px",
					}}
					aria-hidden="true"
				/>
				<div
					className="pointer-events-none absolute -top-40 left-1/4 h-[480px] w-[480px] rounded-full blur-3xl"
					style={{ background: "radial-gradient(circle, rgba(131,137,234,0.35) 0%, transparent 60%)" }}
					aria-hidden="true"
				/>

				<div className="relative">
					<BrandMark dark />
				</div>

				<div className="relative max-w-md">
					<h1 className="text-4xl font-semibold leading-[1.15] tracking-tight text-white">
						Your operations,
						<br />
						from one vantage point.
					</h1>
					<p className="mt-4 text-[15px] leading-relaxed text-ink2-dark">
						Run products, inventory, orders, and forecasts in a single calm, precise surface built for independent
						merchants.
					</p>
					<ul className="mt-10 space-y-4">
						{WORTH.map((item) => (
							<li key={item.text} className="flex items-start gap-3">
								<span className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-md bg-white/10 text-brand-300">
									<item.icon className="size-3.5" />
								</span>
								<span className="text-sm leading-relaxed text-ink2-dark">{item.text}</span>
							</li>
						))}
					</ul>
				</div>

				<div className="relative flex items-center gap-2 text-[13px] text-ink3-dark">
					<ShoppingCart className="size-4" />
					Order orchestration with payment circuit breaking
					<ArrowRight className="size-3.5" />
				</div>
			</div>

			{/* Form panel */}
			<div className="flex min-h-screen items-center justify-center px-6 py-12">
				<div className="w-full max-w-sm animate-fade-up">
					<div className="mb-8 lg:hidden">
						<BrandMark />
					</div>
					<h2 className="text-2xl font-semibold tracking-tight text-ink-light dark:text-ink-dark">{title}</h2>
					{subtitle && <p className="mt-1.5 text-sm text-ink3-light dark:text-ink3-dark">{subtitle}</p>}
					<div className="mt-8">{children}</div>
				</div>
			</div>
		</div>
	);
}
