-- V11__create_ai_conversations.sql
-- AI support chat: conversations, messages, tool calls, usage, documents, embeddings
-- All tables are scoped by tenant_id to enforce multi-tenant isolation at the DB layer.

CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_tenant ON ai_conversations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_tenant_created ON ai_conversations(tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_call_id VARCHAR(128),
    tool_name VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_messages_conv_tenant ON ai_messages(conversation_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_messages_tenant_created ON ai_messages(tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_tool_calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    tool_call_id VARCHAR(128),
    arguments TEXT,
    result TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_calls_msg ON ai_tool_calls(message_id);
CREATE INDEX IF NOT EXISTS idx_ai_tool_calls_conv ON ai_tool_calls(conversation_id, tenant_id);

CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    model VARCHAR(128),
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    cost_usd DECIMAL(10,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_tenant ON ai_usage(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_conv ON ai_usage(conversation_id);
