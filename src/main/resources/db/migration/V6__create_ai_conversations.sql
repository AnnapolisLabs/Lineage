-- Create AI conversations table
CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    messages JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Index for fast lookups by user and chatId
    CONSTRAINT uk_chat_id_user UNIQUE (chat_id, user_id)
);

-- Index for sorting conversations by last updated
CREATE INDEX idx_ai_conversations_user_updated ON ai_conversations(user_id, updated_at DESC);

-- Index for fast chatId lookups
CREATE INDEX idx_ai_conversations_chat_id ON ai_conversations(chat_id);
