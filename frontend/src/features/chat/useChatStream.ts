import { useState, useTransition } from 'react';

type Message = {
  role: 'user' | 'assistant';
  content: string;
};

export function useChatStream() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [isPending, startTransition] = useTransition();

  const sendMessage = (query: string) => {
    if (!query.trim()) return;

    const userMessage: Message = { role: 'user', content: query };
    setMessages((prev) => [...prev, userMessage]);

    setIsStreaming(true);
    const assistantMessage: Message = { role: 'assistant', content: '' };
    setMessages((prev) => [...prev, assistantMessage]);

    const eventSource = new EventSource(`/api/v1/chat/stream?query=${encodeURIComponent(query)}`);

    eventSource.onmessage = (event) => {
      startTransition(() => {
        setMessages((prev) => {
          const last = prev[prev.length - 1];
          if (last.role === 'assistant') {
            const updated = { ...last, content: last.content + event.data };
            return [...prev.slice(0, -1), updated];
          }
          return prev;
        });
      });
    };

    eventSource.onerror = () => {
      eventSource.close();
      setIsStreaming(false);
    };

    eventSource.addEventListener('complete', () => {
      eventSource.close();
      setIsStreaming(false);
    });
  };

  return { messages, isStreaming, isPending, sendMessage };
}
