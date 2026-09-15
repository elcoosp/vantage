package com.vantage.core.chat.infra;

import com.vantage.core.tenant.TenantContext;
import com.vantage.core.tenant.MissingTenantContextException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ingests documents (help articles, policies, product descriptions) into the
 * {@code ai_documents} table for RAG retrieval.
 *
 * <p>In a full implementation this would:
 * 1. Chunk the document text into ~500-token segments
 * 2. Generate embeddings via an embedding model (OpenAI/oLLaMA)
 * 3. Store chunks + vectors in {@code ai_embeddings}
 *
 * For now, documents are stored as-is with content hash for dedup.
 */
@Service
public class DocumentIngestor {

    private final EntityManager entityManager;

    public DocumentIngestor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public AiDocument ingest(String title, String content, String sourceUrl) {
        UUID tenantId = requireTenant();
        String hash = contentHash(content);

        // Dedup: skip if content hash already exists for this tenant
        var existing = entityManager.createNativeQuery(
                "SELECT id FROM ai_documents WHERE content_hash = :hash AND tenant_id = :tenantId")
            .setParameter("hash", hash)
            .setParameter("tenantId", tenantId)
            .getResultList();

        if (!existing.isEmpty()) {
            return null; // Already ingested
        }

        var doc = new AiDocument();
        doc.setTenantId(tenantId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSourceUrl(sourceUrl);
        doc.setContentHash(hash);
        doc.setCreatedAt(Instant.now());
        entityManager.persist(doc);
        entityManager.flush();
        return doc;
    }

    @Transactional
    public int ingestBatch(AiDocumentBatch batch) {
        int count = 0;
        for (AiDocumentEntry entry : batch.documents()) {
            AiDocument doc = ingest(entry.title(), entry.content(), entry.sourceUrl());
            if (doc != null) {
                count++;
            }
        }
        return count;
    }

    private String contentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("No tenant context for document ingestion");
        }
        return tenantId;
    }

    public record AiDocumentBatch(List<AiDocumentEntry> documents) {}

    public record AiDocumentEntry(String title, String content, String sourceUrl) {}
}
