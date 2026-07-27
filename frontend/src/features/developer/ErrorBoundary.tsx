import { Component, type ReactNode } from "react";

interface Props {
	children: ReactNode;
}

interface State {
	hasError: boolean;
	error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = { hasError: false };
	}

	static getDerivedStateFromError(error: Error) {
		return { hasError: true, error };
	}

	componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
		console.error("DeveloperPortal Error:", error, errorInfo);
	}

	render() {
		if (this.state.hasError) {
			return (
				<div className="p-4 bg-red-50 border border-red-200 rounded-lg">
					<h2 className="text-red-800 font-semibold">Something went wrong</h2>
					<p className="text-red-600 text-sm">Please refresh the page or try again later.</p>
				</div>
			);
		}
		return this.props.children;
	}
}
