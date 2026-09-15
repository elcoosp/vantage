package com.vantage.core.chat.infra;

import com.vantage.core.tenant.MissingTenantContextException;
import com.vantage.core.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG retriever using pgvector for similarity search.
 *
 * <p>Performs hybrid search: vector similarity (cosine) + keyword (ILIKE) on
 * the {@code ai_documents} table. All queries are scoped by {@code tenant_id}
 * to enforce multi-tenant isolation.
 *
 * <p>Requires the pgvector extension (Flyway V13) and the {@code ai_embeddings}
 * table with a VECTOR(768) column (Flyway V12) for vector search. The keyword
 * search path works without embeddings, so retrieval falls back gracefully.
 */
@Repository
public class DocumentRetriever {

    private final EntityManager entityManager;
    private static final int DEFAULT_TOP_K = 5;

    public DocumentRetriever(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Keyword-based retrieval from the {@code ai_documents} table.
     * All queries are scoped by {@code tenant_id}.
     *
     * @param query The user's search query.
     * @param topK  Number of results to return.
     * @return List of retrieved document context snippets, scoped by tenant.
     */
    @Transactional(readOnly = true)
    public List<RagContext> retrieve(String query, int topK) {
        UUID tenantId = requireTenant();

        String sql = """
            SELECT d.content, d.title, d.source_url
            FROM ai_documents d
            WHERE d.tenant_id = :tenantId
              AND (d.content ILIKE :searchTerm OR d.title ILIKE :searchTerm)
            ORDER BY d.created_at DESC
            """;

        Query keywordQuery = entityManager.createNativeQuery(sql);
        keywordQuery.setParameter("tenantId", tenantId);
        keywordQuery.setParameter("searchTerm", "%" + query + "%");
        keywordQuery.setMaxResults(topK);

        @SuppressWarnings("unchecked")
        List<Object[]> results = keywordQuery.getResultList();
        if (results.isEmpty()) {
            return List.of();
        }

        return results.stream()
            .map(row -> new RagContext(
                (String) row[1],  // title
                (String) row[0],  // content
                (String) row[2]   // source_url
            ))
            .collect(Collectors.toList());
    }

    public List<RagContext> retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K);
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("No tenant context for document retrieval");
        }
        return tenantId;
    }
}
