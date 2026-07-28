import { useState } from "react";
import { useChatStream } from "./useChatStream";

export function ChatWidget() {
	const [isOpen, setIsOpen] = useState(false);
	const [input, setInput] = useState("");
	const { messages, isStreaming, sendMessage } = useChatStream();

	const handleSend = () => {
		if (!input.trim() || isStreaming) return;
		sendMessage(input);
		setInput("");
	};

	const toggleOpen = () => setIsOpen((prev) => !prev);

	return (
		<div className="fixed bottom-4 right-4 z-50">
			{!isOpen ? (
				<button
					onClick={toggleOpen}
					className="w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center transition-colors"
					aria-label="Open chat"
				>
					<svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
						<path
							strokeLinecap="round"
							strokeLinejoin="round"
							strokeWidth={2}
							d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
						/>
					</svg>
				</button>
			) : (
				<div className="w-96 h-[600px] bg-gray-900 rounded-2xl shadow-2xl border border-gray-700 flex flex-col overflow-hidden">
					<div className="flex items-center justify-between p-4 border-b border-gray-700 bg-gray-800">
						<h3 className="text-white font-semibold">Support Assistant</h3>
						<button
							onClick={toggleOpen}
							className="text-gray-400 hover:text-white transition-colors"
							aria-label="Close chat"
						>
							<svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
							</svg>
						</button>
					</div>

					<div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
						{messages.map((msg, idx) => (
							<div key={idx} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
								<div
									className={`max-w-[80%] px-4 py-2 rounded-lg ${
										msg.role === "user"
											? "bg-blue-600 text-white rounded-br-none"
											: "bg-gray-700 text-gray-100 rounded-bl-none"
									}`}
								>
									{msg.content}
									{isStreaming && msg.role === "assistant" && idx === messages.length - 1 && (
										<span className="inline-block w-2 h-4 ml-1 bg-white animate-pulse" />
									)}
								</div>
							</div>
						))}
						{isStreaming && messages.length === 0 && (
							<div className="flex justify-start">
								<div className="bg-gray-700 text-gray-100 px-4 py-2 rounded-lg rounded-bl-none">
									<span className="inline-block w-2 h-4 bg-white animate-pulse" />
								</div>
							</div>
						)}
					</div>

					<div className="p-4 border-t border-gray-700 bg-gray-800">
						<div className="flex gap-2">
							<input
								type="text"
								value={input}
								onChange={(e) => setInput(e.target.value)}
								onKeyDown={(e) => e.key === "Enter" && handleSend()}
								placeholder="Ask a question..."
								disabled={isStreaming}
								className="flex-1 px-3 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
							/>
							<button
								onClick={handleSend}
								disabled={!input.trim() || isStreaming}
								className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg disabled:opacity-50 transition-colors"
							>
								Send
							</button>
						</div>
					</div>
				</div>
			)}
		</div>
	);
}
