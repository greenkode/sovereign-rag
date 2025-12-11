CREATE TABLE IF NOT EXISTS conversation_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id VARCHAR(255) NOT NULL,
    organization_id UUID,
    knowledge_base_id UUID,
    message_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sequence_number INT NOT NULL DEFAULT 0,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by UUID
);

CREATE INDEX IF NOT EXISTS idx_conversation_id ON conversation_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_created ON conversation_message(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_conversation_org ON conversation_message(organization_id);
CREATE INDEX IF NOT EXISTS idx_conversation_kb ON conversation_message(knowledge_base_id);
