import { useState, useTransition } from "react";

type Message = {
	id: string;
	role: "user" | "assistant";
	content: string;
};

type Citation = {
	documentTitle: string;
	snippet: string;
	sourceUrl: string;
};

type ChatEvent =
	| { type: "content"; text: string }
	| { type: "tool_call"; id: string; name: string; arguments?: string }
	| { type: "tool_result"; toolCallId: string; content: string }
	| { type: "citation"; documentTitle: string; snippet: string; sourceUrl: string; chunkIndex?: number }
	| { type: "usage"; model: string }
	| { type: "error"; message: string; code: string }
	| { type: "done"};

export function useChatStream() {
	const [messages, setMessages] = useState<Message[]>([]);
	const [citations, setCitations] = useState<Citation[]>([]);
	const [isStreaming, setIsStreaming] = useState(false);
	const [isPending, startTransition] = useTransition();

	const sendMessage = (query: string, conversationId?: string) => {
		if (!query.trim()) return;

		const userMessage: Message = { id: crypto.randomUUID(), role: "user", content: query };
		setMessages((prev) => [...prev, userMessage]);
		setCitations([]);

		setIsStreaming(true);
		const assistantMessage: Message = { id: crypto.randomUUID(), role: "assistant", content: "" };
		setMessages((prev) => [...prev, assistantMessage]);

		const body: { query: string; conversationId?: string } = { query };
		if (conversationId) body.conversationId = conversationId;

		const controller = new AbortController();

		fetch("/api/v1/chat/stream", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
			},
			body: JSON.stringify(body),
			signal: controller.signal,
		}).then((response) => {
			if (!response.ok) {
				throw new Error(`Chat request failed: ${response.status}`);
			}

			const reader = response.body?.getReader();
			if (!reader) {
				throw new Error("Response body is not readable");
			}
			const decoder = new TextDecoder("utf-8");
			let buffer = "";

			const readChunk = () => {
				reader.read().then(({ done, value }) => {
					if (done) {
						setIsStreaming(false);
						return;
					}
					buffer += decoder.decode(value, { stream: true });

					// SSE lines are separated by \n\n
					const lines = buffer.split("\n\n");
					buffer = lines.pop() || ""; // keep incomplete trailing line

					for (const line of lines) {
						if (line.startsWith("data: ")) {
							const jsonStr = line.slice(6);
							try {
								const event: ChatEvent = JSON.parse(jsonStr);
								startTransition(() => {
									setMessages((prev) => {
										const last = prev[prev.length - 1];
										if (event.type === "content" && last && last.role === "assistant") {
											return [...prev.slice(0, -1), { ...last, content: last.content + event.text }];
										}
										if (event.type === "error") {
											return [...prev, { id: crypto.randomUUID(), role: "assistant", content: `[Error: ${event.message}]` }];
										}
										if (event.type === "citation") {
											setCitations((prev) => [...prev, {
												documentTitle: event.documentTitle,
												snippet: event.snippet,
												sourceUrl: event.sourceUrl,
											}]);
											return prev;
										}
										if (event.type === "tool_result" && last && last.role === "assistant") {
											return [...prev.slice(0, -1), { ...last, content: last.content + `\n[Tool: ${event.content}]` }];
										}
										return prev;
									});
								});
							} catch (e) {
								console.error("Failed to parse SSE event:", jsonStr, e);
							}
						}
					}

					readChunk();
				}).catch((err) => {
					if (err.name !== "AbortError") {
						console.error("Stream error:", err);
						setIsStreaming(false);
					}
				});
			};

			readChunk();
		}).catch((err) => {
			console.error("Chat error:", err);
			setIsStreaming(false);
			setMessages((prev) => [...prev, {
				id: crypto.randomUUID(),
				role: "assistant",
				content: `[Error: ${err.message}]`,
			}]);
		});

		// Return a cancel function so the caller can abort the request
		return () => controller.abort();
	};

	return { messages, citations, isStreaming, isPending, sendMessage };
}
