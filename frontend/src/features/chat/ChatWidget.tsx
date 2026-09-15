import { Menu, Send, Sparkles, X } from "lucide-react";
import { useState } from "react";
import { useChatStream } from "./useChatStream";

export function ChatWidget() {
	const [isOpen, setIsOpen] = useState(false);
	const [input, setInput] = useState("");
	const { messages, citations, isStreaming, sendMessage } = useChatStream();

	const handleSend = () => {
		if (!input.trim() || isStreaming) return;
		sendMessage(input);
		setInput("");
	};

	const toggleOpen = () => setIsOpen((prev) => !prev);

	return (
		<div className="fixed bottom-4 right-4 z-50 flex flex-col items-end gap-3">
			{isOpen && (
				<div className="flex h-[560px] w-[380px] flex-col overflow-hidden rounded-2xl border border-line-light bg-card-light shadow-float animate-scale-in dark:border-line-dark dark:bg-card-dark">
					<div className="flex items-center justify-between border-b border-line-light bg-card2-light/70 px-4 py-3 dark:border-line-dark dark:bg-card2-dark/70">
						<div className="flex items-center gap-2.5">
							<span className="flex size-8 items-center justify-center rounded-lg bg-brand-500/15 text-brand-600 dark:bg-brand-500/20 dark:text-brand-300">
								<Sparkles className="size-4" />
							</span>
							<div>
								<h3 className="text-[13px] font-semibold text-ink-light dark:text-ink-dark">Support Assistant</h3>
								<p className="text-[11px] text-ink3-light dark:text-ink3-dark">Vantage support &middot; operational Q&amp;A</p>
							</div>
						</div>
						<button
							type="button"
							onClick={toggleOpen}
							className="rounded-md p-1.5 text-ink3-light transition-colors hover:bg-card2-light hover:text-ink-light dark:text-ink3-dark dark:hover:bg-card2-dark dark:hover:text-ink-dark"
							aria-label="Close chat"
						>
							<X className="size-4" />
						</button>
					</div>

					<div className="custom-scrollbar flex-1 space-y-3 overflow-y-auto p-4">
						{messages.map((msg) => (
							<div
								key={msg.id}
								className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}
							>
								<div
									className={`max-w-[80%] rounded-2xl px-3.5 py-2 text-[13px] leading-relaxed ${
										msg.role === "user"
											? "rounded-br-md bg-brand-600 text-white"
											: "rounded-bl-md border border-line-light bg-card2-light text-ink-light dark:border-line-dark dark:bg-card2-dark dark:text-ink-dark"
									}`}
								>
									{msg.content}
									{isStreaming &&
										msg.role === "assistant" &&
										msg.id === messages[messages.length - 1]?.id && (
											<span className="ml-1 inline-block h-3 w-[2px] animate-pulse bg-brand-500 align-middle" />
										)}
								</div>
							</div>
						))}
						{isStreaming && messages.length === 0 && (
							<div className="flex justify-start">
								<div className="rounded-2xl rounded-bl-md border border-line-light bg-card2-light px-3.5 py-2 dark:border-line-dark dark:bg-card2-dark">
									<span className="inline-block h-3 w-[2px] animate-pulse bg-brand-500" />
								</div>
							</div>
						)}
						{citations.length > 0 && (
							<div className="mt-2 space-y-1 border-t border-line-light pt-2 dark:border-line-dark">
								{citations.map((citation, i) => (
									<a
										key={i}
										href={citation.sourceUrl}
										className="block text-[11px] text-ink3-light hover:underline"
									>
										<span className="font-medium text-ink-light dark:text-ink-dark">[{citation.documentTitle}]</span>
										<span className="ml-1 truncate">
											{citation.snippet.slice(0, 80)}
											{citation.snippet.length > 80 ? "…" : ""}
										</span>
									</a>
								))}
							</div>
						)}
					</div>

					<div className="border-t border-line-light bg-card2-light/50 p-3 dark:border-line-dark dark:bg-card2-dark/50">
						<div className="flex gap-2">
							<input
								type="text"
								value={input}
								onChange={(e) => setInput(e.target.value)}
								onKeyDown={(e) => e.key === "Enter" && handleSend()}
								placeholder="Ask about orders, inventory, forecasts…"
								disabled={isStreaming}
								className="h-9 flex-1 rounded-lg border border-line-light bg-card-light px-3 text-[13px] text-ink-light outline-none transition-shadow placeholder:text-ink3-light focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 disabled:opacity-50 dark:border-line-dark dark:bg-card-dark dark:text-ink-dark dark:placeholder:text-ink3-dark"
							/>
							<button
								type="button"
								onClick={handleSend}
								disabled={!input.trim() || isStreaming}
								className="inline-flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-600 text-white transition-colors hover:bg-brand-700 disabled:opacity-40"
								aria-label="Send message"
							>
								<Send className="size-4" />
							</button>
						</div>
					</div>
				</div>
			)}

			<button
				type="button"
				onClick={toggleOpen}
				className="flex size-14 items-center justify-center rounded-full bg-brand-600 text-white shadow-pop transition-all hover:-translate-y-0.5 hover:bg-brand-700"
				aria-label={isOpen ? "Close chat" : "Open chat"}
			>
				{isOpen ? <X className="size-6" /> : <Menu className="size-6" />}
			</button>
		</div>
	);
}
