import React from 'react';

interface MarkdownTextProps {
  content: string;
}

// Basic markdown renderer without external dependencies
function renderMarkdown(text: string): string {
  // Convert markdown to HTML with basic formatting
  let html = text
    // Headers
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // Bold and italic
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // Links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    // Unordered lists
    .replace(/^\s*-\s+(.*)$/gim, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    // Line breaks
    .replace(/\n/g, '<br />');

  return html;
}

export function MarkdownText({ content }: MarkdownTextProps) {
  const html = renderMarkdown(content);
  return <div className="prose prose-slate max-w-none" dangerouslySetInnerHTML={{ __html: html }} />;
}
