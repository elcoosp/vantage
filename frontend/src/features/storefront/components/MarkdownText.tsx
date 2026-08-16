interface MarkdownTextProps {
	content: string;
}

// Basic markdown renderer without external dependencies
function renderMarkdown(text: string): string {
	// Escape HTML first so user content cannot inject raw markup (XSS).
	const escaped = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

	// Convert markdown to HTML with basic formatting (operates on escaped text).
	const html = escaped
		// Headers
		.replace(/^### (.*$)/gim, "<h3>$1</h3>")
		.replace(/^## (.*$)/gim, "<h2>$1</h2>")
		.replace(/^# (.*$)/gim, "<h1>$1</h1>")
		// Bold and italic
		.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
		.replace(/\*(.*?)\*/g, "<em>$1</em>")
		// Links
		.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
		// Unordered lists
		.replace(/^\s*-\s+(.*)$/gim, "<li>$1</li>")
		.replace(/(<li>.*<\/li>)/s, "<ul>$1</ul>")
		// Line breaks
		.replace(/\n/g, "<br />");

	return html;
}

export function MarkdownText({ content }: MarkdownTextProps) {
	const html = renderMarkdown(content);
	// NOTE: `noDangerouslySetInnerHTML` is intentionally disabled for this file
	// (see biome.json overrides). renderMarkdown() HTML-escapes all user input
	// ('<', '>', '&') before producing markup, so the injected HTML is safe.
	return <div className="prose prose-slate max-w-none" dangerouslySetInnerHTML={{ __html: html }} />;
}
