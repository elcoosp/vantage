import type {
	ButtonHTMLAttributes,
	HTMLAttributes,
	InputHTMLAttributes,
	LabelHTMLAttributes,
	ReactNode,
	SelectHTMLAttributes,
} from "react";

/* ------------------------------------------------------------------ */
/* cx — tiny class combiner                                            */
/* ------------------------------------------------------------------ */

export function cx(...parts: Array<string | false | null | undefined>): string {
	return parts.filter(Boolean).join(" ");
}

/* ------------------------------------------------------------------ */
/* Surfaces                                                            */
/* ------------------------------------------------------------------ */

const card =
	"rounded-xl border border-line-light bg-card-light text-ink-light shadow-card dark:border-line-dark dark:bg-card-dark dark:text-ink-dark";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
	hover?: boolean;
}

export function Card({ className, hover = false, ...props }: CardProps) {
	return (
		<div
			{...props}
			className={cx(card, hover && "transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lift", className)}
		/>
	);
}

export function CardHeader({
	title,
	subtitle,
	action,
	className,
}: {
	title: ReactNode;
	subtitle?: ReactNode;
	action?: ReactNode;
	className?: string;
}) {
	return (
		<div className={cx("flex items-start justify-between gap-4 px-5 pt-5 pb-4", className)}>
			<div className="min-w-0">
				<h3 className="text-[15px] font-semibold tracking-tight text-ink-light dark:text-ink-dark">{title}</h3>
				{subtitle && (
					<p className="mt-0.5 text-[13px] leading-relaxed text-ink3-light dark:text-ink3-dark">{subtitle}</p>
				)}
			</div>
			{action}
		</div>
	);
}

/* ------------------------------------------------------------------ */
/* Buttons                                                             */
/* ------------------------------------------------------------------ */

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize = "sm" | "md";

const baseButton =
	"inline-flex select-none items-center justify-center gap-1.5 whitespace-nowrap rounded-lg font-medium transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/50 disabled:cursor-not-allowed disabled:opacity-50";

const buttonVariants: Record<ButtonVariant, string> = {
	primary: "bg-brand-600 text-white shadow-card hover:bg-brand-700 active:bg-brand-800",
	secondary:
		"border border-line-light bg-card-light text-ink-light hover:bg-card2-light active:bg-card2-light dark:border-line-dark dark:bg-card-dark dark:text-ink-dark dark:hover:bg-card2-dark",
	ghost:
		"text-ink2-light hover:bg-card2-light hover:text-ink-light dark:text-ink2-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark",
	danger:
		"text-status-danger-ink-light bg-status-danger-soft-light hover:bg-red-100 dark:text-[#FF9A9D] dark:bg-[#E5484D]/15 dark:hover:bg-[#E5484D]/25",
};

const buttonSizes: Record<ButtonSize, string> = {
	sm: "h-8 px-3 text-[13px]",
	md: "h-9 px-4 text-sm",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	variant?: ButtonVariant;
	size?: ButtonSize;
}

export function Button({ variant = "primary", size = "md", className, ...props }: ButtonProps) {
	return <button {...props} className={cx(baseButton, buttonVariants[variant], buttonSizes[size], className)} />;
}

export function IconButton({ variant = "ghost", className, ...props }: ButtonProps & { label: string }) {
	return (
		<button
			{...props}
			aria-label={props.label}
			className={cx(
				"inline-flex h-8 w-8 items-center justify-center rounded-lg transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/50 disabled:cursor-not-allowed disabled:opacity-50",
				buttonVariants[variant],
				className,
			)}
		/>
	);
}

/* ------------------------------------------------------------------ */
/* Fields & controls                                                   */
/* ------------------------------------------------------------------ */

const fieldControl =
	"w-full rounded-lg border border-line-light bg-card-light px-3 text-sm text-ink-light shadow-[inset_0_1px_2px_rgba(23,24,28,0.03)] transition-all duration-150 placeholder:text-ink3-light hover:border-line2-light focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/15 dark:border-line-dark dark:bg-card-dark dark:text-ink-dark dark:placeholder:text-ink3-dark dark:hover:border-line2-dark";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {}

export function Input({ className, ...props }: InputProps) {
	return <input {...props} className={cx(fieldControl, "h-9", className)} />;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {}

export function Select({ className, children, ...props }: SelectProps) {
	return (
		<select {...props} className={cx(fieldControl, "h-9 cursor-pointer appearance-none bg-no-repeat", className)}>
			{children}
		</select>
	);
}

export function Field({ label, children, className }: { label: ReactNode; children: ReactNode; className?: string }) {
	return (
		// biome-ignore lint/a11y/noLabelWithoutControl: label wraps its child control
		<label className={cx("block", className)}>
			<span className="mb-1.5 block text-[13px] font-medium text-ink2-light dark:text-ink2-dark">{label}</span>
			{children}
		</label>
	);
}

interface LabelProps extends LabelHTMLAttributes<HTMLLabelElement> {
	children: ReactNode;
}

export function Label({ children, className, ...props }: LabelProps) {
	return (
		// biome-ignore lint/a11y/noLabelWithoutControl: label wraps its child control
		<label {...props} className={cx("text-[13px] font-medium text-ink2-light dark:text-ink2-dark", className)}>
			{children}
		</label>
	);
}

/* ------------------------------------------------------------------ */
/* Status badge                                                        */
/* ------------------------------------------------------------------ */

export type Tone = "neutral" | "info" | "violet" | "success" | "warning" | "danger";

const toneStyles: Record<Tone, { chip: string; dot: string }> = {
	neutral: {
		chip: "bg-card2-light text-ink2-light dark:bg-card2-dark dark:text-ink2-dark",
		dot: "bg-ink3-light dark:bg-ink3-dark",
	},
	info: {
		chip: "bg-status-info-soft-light text-status-info-ink-light dark:bg-[#5B7CFA]/15 dark:text-[#A9BBFF]",
		dot: "bg-status-info-solid-light dark:bg-[#6685FF]",
	},
	violet: {
		chip: "bg-[#EEECFB] text-[#5B4FD6] dark:bg-[#8B7CF8]/15 dark:text-[#B1A7FF]",
		dot: "bg-[#6B5CE7] dark:bg-[#8B7CF8]",
	},
	success: {
		chip: "bg-status-success-soft-light text-status-success-ink-light dark:bg-[#1EAD72]/15 dark:text-[#5CE3A1]",
		dot: "bg-status-success-solid-light dark:bg-[#2DBD7B]",
	},
	warning: {
		chip: "bg-status-warning-soft-light text-status-warning-ink-light dark:bg-[#D3932B]/15 dark:text-[#F0C468]",
		dot: "bg-status-warning-solid-light dark:bg-[#D3932B]",
	},
	danger: {
		chip: "bg-status-danger-soft-light text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]",
		dot: "bg-status-danger-solid-light dark:bg-[#E5484D]",
	},
};

export function StatusBadge({
	tone = "neutral",
	children,
	className,
}: { tone?: Tone; children: ReactNode; className?: string }) {
	const styles = toneStyles[tone];
	return (
		<span
			className={cx(
				"inline-flex items-center gap-1.5 rounded-full py-1 pl-2.5 pr-3 text-[12px] font-medium",
				styles.chip,
				className,
			)}
		>
			<span className={cx("size-1.5 rounded-full", styles.dot)} />
			{children}
		</span>
	);
}

const statusToneByOrder: Record<string, Tone> = {
	CREATED: "info",
	CONFIRMED: "violet",
	PAID: "success",
	SHIPPED: "warning",
	DELIVERED: "success",
	CANCELLED: "danger",
	IN_PROGRESS: "info",
	FULFILLED: "violet",
	OPEN: "info",
	ACTIVE: "success",
};

export const statusLabel: Record<string, string> = {
	CREATED: "Created",
	CONFIRMED: "Confirmed",
	PAID: "Paid",
	SHIPPED: "Shipped",
	DELIVERED: "Delivered",
	CANCELLED: "Cancelled",
	IN_PROGRESS: "In progress",
	FULFILLED: "Fulfilled",
	OPEN: "Open",
	ACTIVE: "Active",
};

export function orderTone(status: string): Tone {
	return statusToneByOrder[status] ?? "neutral";
}

/* ------------------------------------------------------------------ */
/* Modal                                                               */
/* ------------------------------------------------------------------ */

interface ModalProps {
	open: boolean;
	onClose: () => void;
	title?: ReactNode;
	description?: ReactNode;
	children: ReactNode;
	className?: string;
}

export function Modal({ open, onClose, title, description, children, className }: ModalProps) {
	if (!open) return null;
	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto p-4">
			{/* biome-ignore lint/a11y/useKeyWithClickEvents: backdrop is aria-hidden, keyboard users escape via Esc */}
			<div
				className="absolute inset-0 bg-ink-light/50 backdrop-blur-[2px] dark:bg-black/60"
				onClick={onClose}
				aria-hidden="true"
			/>
			{/* biome-ignore lint/a11y/useSemanticElements: native dialog requires inert fallback handling not desired here */}
			<div
				role="dialog"
				aria-modal="true"
				className={cx(
					"relative z-10 w-full max-w-md animate-scale-in rounded-2xl border border-line-light bg-card-light p-6 shadow-float dark:border-line-dark dark:bg-card-dark",
					className,
				)}
			>
				{title && <h2 className="text-lg font-semibold tracking-tight text-ink-light dark:text-ink-dark">{title}</h2>}
				{description && <p className="mt-1 text-sm text-ink3-light dark:text-ink3-dark">{description}</p>}
				<div className={title ? "mt-5" : undefined}>{children}</div>
			</div>
		</div>
	);
}

/* ------------------------------------------------------------------ */
/* Page scaffolding                                                    */
/* ------------------------------------------------------------------ */

export function PageHeader({
	title,
	description,
	actions,
	className,
}: {
	title: ReactNode;
	description?: ReactNode;
	actions?: ReactNode;
	className?: string;
}) {
	return (
		<div className={cx("flex flex-wrap items-end justify-between gap-4", className)}>
			<div className="min-w-0">
				<h1 className="text-[26px] font-semibold leading-tight tracking-tight text-ink-light dark:text-ink-dark">
					{title}
				</h1>
				{description && (
					<p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-ink3-light dark:text-ink3-dark">{description}</p>
				)}
			</div>
			{actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
		</div>
	);
}

export function Metric({
	label,
	value,
	hint,
	icon,
	delta,
	className,
}: {
	label: ReactNode;
	value: ReactNode;
	hint?: ReactNode;
	icon?: ReactNode;
	delta?: ReactNode;
	className?: string;
}) {
	return (
		<Card className={cx("px-5 py-4", className)}>
			<div className="flex items-center justify-between gap-3">
				<span className="text-[13px] font-medium text-ink3-light dark:text-ink3-dark">{label}</span>
				{icon && <span className="text-ink3-light dark:text-ink3-dark">{icon}</span>}
			</div>
			<div className="mt-2 flex items-baseline gap-2">
				<span className="text-2xl font-semibold tabular-nums tracking-tight text-ink-light dark:text-ink-dark">
					{value}
				</span>
				{delta}
			</div>
			{hint && <p className="mt-1 text-xs text-ink3-light dark:text-ink3-dark">{hint}</p>}
		</Card>
	);
}

export function EmptyState({
	icon,
	title,
	description,
	action,
	className,
}: {
	icon?: ReactNode;
	title: ReactNode;
	description?: ReactNode;
	action?: ReactNode;
	className?: string;
}) {
	return (
		<div className={cx("flex flex-col items-center justify-center px-6 py-16 text-center", className)}>
			{icon && (
				<div className="mb-4 flex size-12 items-center justify-center rounded-xl bg-card2-light text-ink3-light dark:bg-card2-dark dark:text-ink3-dark">
					{icon}
				</div>
			)}
			<h3 className="text-[15px] font-semibold text-ink-light dark:text-ink-dark">{title}</h3>
			{description && (
				<p className="mt-1.5 max-w-sm text-sm leading-relaxed text-ink3-light dark:text-ink3-dark">{description}</p>
			)}
			{action && <div className="mt-5">{action}</div>}
		</div>
	);
}

export function Kbd({ children, className }: { children: ReactNode; className?: string }) {
	return (
		<kbd
			className={cx(
				"inline-flex h-5 min-w-5 items-center justify-center rounded-[5px] border border-line-light bg-card2-light px-1 font-mono text-[11px] font-medium text-ink3-light dark:border-line-dark dark:bg-card2-dark dark:text-ink3-dark",
				className,
			)}
		>
			{children}
		</kbd>
	);
}

/* ------------------------------------------------------------------ */
/* Table primitives                                                    */
/* ------------------------------------------------------------------ */

export function TableCard({ children, className }: { children: ReactNode; className?: string }) {
	return <Card className={cx("overflow-hidden", className)}>{children}</Card>;
}

export function Table({ children, className }: { children: ReactNode; className?: string }) {
	return (
		<div className={cx("overflow-x-auto", className)}>
			<table className="w-full border-collapse text-left">{children}</table>
		</div>
	);
}

export function THead({ children }: { children: ReactNode }) {
	return (
		<thead>
			<tr className="border-b border-line-light bg-card2-light/60 dark:border-line-dark dark:bg-card2-dark/40">
				{children}
			</tr>
		</thead>
	);
}

export function Th({ children, className }: { children: ReactNode; className?: string }) {
	return (
		<th
			className={cx(
				"px-5 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-ink3-light dark:text-ink3-dark",
				className,
			)}
		>
			{children}
		</th>
	);
}

export function Tr({ children, className }: { children: ReactNode; className?: string }) {
	return (
		<tr
			className={cx(
				"border-b border-line-light transition-colors last:border-0 hover:bg-card2-light/50 dark:border-line-dark dark:hover:bg-card2-dark/40",
				className,
			)}
		>
			{children}
		</tr>
	);
}

export function Td({ children, className }: { children: ReactNode; className?: string }) {
	return <td className={cx("px-5 py-3 text-sm text-ink2-light dark:text-ink2-dark", className)}>{children}</td>;
}
