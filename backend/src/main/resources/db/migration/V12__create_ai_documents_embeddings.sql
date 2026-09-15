-- V12__create_ai_documents_embeddings.sql
-- RAG support: documents table for help docs, policies, product descriptions
-- plus a vector embeddings table for pgvector (HNSW index).

-- Documents table (content is ingested text)
CREATE TABLE IF NOT EXISTS ai_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255),
    content TEXT,
    source_url VARCHAR(511),
    content_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_documents_tenant ON ai_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_documents_hash ON ai_documents(content_hash);

-- Embeddings table: one row per document chunk with a 768-dim vector
-- Requires the pgvector extension (enabled in V13).
CREATE TABLE IF NOT EXISTS ai_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_embeddings_tenant ON ai_embeddings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_embeddings_doc ON ai_embeddings(document_id);
-- HNSW index for fast similarity search
CREATE INDEX IF NOT EXISTS idx_ai_embeddings_hnsw ON ai_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
