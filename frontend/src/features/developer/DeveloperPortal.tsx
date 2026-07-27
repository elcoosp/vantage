import { useState } from "react";
import { ApiKeysPanel } from "./ApiKeysPanel";
import { ApiLogStream } from "./ApiLogStream";
import { ErrorBoundary } from "./ErrorBoundary";
import { WebhooksPanel } from "./WebhooksPanel";

type Tab = "apiKeys" | "webhooks" | "logs";

export function DeveloperPortal() {
	const [activeTab, setActiveTab] = useState<Tab>("apiKeys");

	return (
		<ErrorBoundary>
			<div className="p-6 space-y-6">
				<h2 className="text-2xl font-bold text-gray-800 dark:text-gray-200">Developer Portal</h2>
				<p className="text-gray-600 dark:text-gray-400">
					Manage your API keys, webhook endpoints, and monitor API activity.
				</p>

				{/* Tabs */}
				<div className="border-b border-gray-200 dark:border-gray-700">
					<nav className="-mb-px flex space-x-8">
						{[
							{ id: "apiKeys", label: "API Keys" },
							{ id: "webhooks", label: "Webhooks" },
							{ id: "logs", label: "Live Logs" },
						].map((tab) => (
							<button
								type="button"
								key={tab.id}
								onClick={() => setActiveTab(tab.id as Tab)}
								className={`
                  py-2 px-1 border-b-2 font-medium text-sm
                  ${
										activeTab === tab.id
											? "border-blue-500 text-blue-600 dark:text-blue-400"
											: "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
									}
                `}
							>
								{tab.label}
							</button>
						))}
					</nav>
				</div>

				{/* Panels */}
				<div className="mt-6">
					{activeTab === "apiKeys" && <ApiKeysPanel />}
					{activeTab === "webhooks" && <WebhooksPanel />}
					{activeTab === "logs" && <ApiLogStream />}
				</div>
			</div>
		</ErrorBoundary>
	);
}
