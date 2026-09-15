package com.vantage.core.chat.infra;

import com.vantage.core.domain.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * An ingested document used for RAG (Retrieval-Augmented Generation).
 * Embeddings are stored in a separate {@link AiEmbedding} row with a vector column
 * consumable by HNSW similarity search.
 */
@Entity
@Table(name = "ai_documents")
@FilterDef(name = "tenantFilter_AiDocument", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter_AiDocument", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
public class AiDocument extends BaseTenantEntity {

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 511)
    private String sourceUrl;

    @Column(name = "content_hash", length = 64)
    private String contentHash;
}
