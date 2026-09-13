import { KeyRound, Radio, Webhook } from "lucide-react";
import { useState } from "react";
import { PageHeader } from "../../components/ui";
import { ApiKeysPanel } from "./ApiKeysPanel";
import { ApiLogStream } from "./ApiLogStream";
import { ErrorBoundary } from "./ErrorBoundary";
import { WebhooksPanel } from "./WebhooksPanel";

type Tab = "apiKeys" | "webhooks" | "logs";

const TABS: Array<{ id: Tab; label: string; icon: typeof KeyRound }> = [
	{ id: "apiKeys", label: "API keys", icon: KeyRound },
	{ id: "webhooks", label: "Webhooks", icon: Webhook },
	{ id: "logs", label: "Live logs", icon: Radio },
];

export function DeveloperPortal() {
	const [activeTab, setActiveTab] = useState<Tab>("apiKeys");

	return (
		<ErrorBoundary>
			<div className="space-y-6 animate-fade-up">
				<PageHeader
					title="Developer"
					description="Integrate with Vantage — manage API keys, webhook endpoints, and monitor live API activity."
				/>

				<div
					className="inline-flex rounded-lg border border-line-light bg-card-light p-1 shadow-card dark:border-line-dark dark:bg-card-dark"
					role="tablist"
					aria-label="Developer sections"
				>
					{TABS.map((tab) => (
						<button
							type="button"
							key={tab.id}
							role="tab"
							aria-selected={activeTab === tab.id}
							onClick={() => setActiveTab(tab.id)}
							className={`flex h-8 items-center gap-1.5 rounded-md px-3 text-[13px] font-medium transition-colors ${
								activeTab === tab.id
									? "bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-100"
									: "text-ink3-light hover:text-ink-light dark:text-ink3-dark dark:hover:text-ink-dark"
							}`}
						>
							<tab.icon className={`size-3.5 ${activeTab === tab.id ? "text-brand-600 dark:text-brand-300" : ""}`} />
							{tab.label}
						</button>
					))}
				</div>

				<div role="tabpanel">
					{activeTab === "apiKeys" && <ApiKeysPanel />}
					{activeTab === "webhooks" && <WebhooksPanel />}
					{activeTab === "logs" && <ApiLogStream />}
				</div>
			</div>
		</ErrorBoundary>
	);
}
