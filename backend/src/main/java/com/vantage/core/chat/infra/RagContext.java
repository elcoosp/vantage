package com.vantage.core.chat.infra;

/**
 * A single retrieved document chunk used for RAG context.
 *
 * @param title    document title
 * @param content  the relevant text snippet
 * @param sourceUrl  source URL or path for citation
 */
public record RagContext(
    String title,
    String content,
    String sourceUrl
) {}
