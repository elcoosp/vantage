-- V13__enable_pgvector.sql
-- Enables the pgvector extension for AI_embeddings.vector column.
-- Must run in the same database where ai_embeddings was created (V12).

CREATE EXTENSION IF NOT EXISTS vector;
