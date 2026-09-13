import { Component, type ReactNode } from "react";

interface Props {
	children: ReactNode;
}

interface State {
	hasError: boolean;
	error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
	state: State = { hasError: false, error: null };

	static getDerivedStateFromError(error: Error): State {
		return { hasError: true, error };
	}

	handleReset = () => {
		this.setState({ hasError: false, error: null });
	};

	render() {
		if (this.state.hasError) {
			return (
				<div className="mx-auto mt-6 max-w-md rounded-xl border border-line-light bg-card-light p-6 text-center shadow-card dark:border-line-dark dark:bg-card-dark">
					<div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-xl bg-status-danger-soft-light text-status-danger-ink-light dark:bg-[#E5484D]/15 dark:text-[#FF9A9D]">
						<svg
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							strokeWidth="2"
							className="size-5"
							role="img"
							aria-label="Error"
						>
							<circle cx="12" cy="12" r="10" />
							<path d="M12 8v4M12 16h.01" strokeLinecap="round" />
						</svg>
					</div>
					<h2 className="text-[15px] font-semibold text-ink-light dark:text-ink-dark">Something went wrong</h2>
					<p className="mt-1.5 break-words text-[13px] text-ink3-light dark:text-ink3-dark">
						{this.state.error?.message ?? "An unexpected error occurred."}
					</p>
					<button
						type="button"
						onClick={this.handleReset}
						className="mt-5 inline-flex h-9 items-center justify-center rounded-lg bg-brand-600 px-4 text-[13px] font-medium text-white transition-colors hover:bg-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 focus-visible:ring-offset-card-light dark:focus-visible:ring-offset-card-dark"
					>
						Try again
					</button>
				</div>
			);
		}
		return this.props.children;
	}
}
