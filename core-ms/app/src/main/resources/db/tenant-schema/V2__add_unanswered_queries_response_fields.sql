-- Add missing columns to unanswered_queries table
-- These columns are needed to track the response given to users and whether general knowledge was used

-- Add response column to store the actual response given to the user
ALTER TABLE unanswered_queries
ADD COLUMN IF NOT EXISTS response TEXT;

-- Add used_general_knowledge column to track if the response used general LLM knowledge vs knowledge base
ALTER TABLE unanswered_queries
ADD COLUMN IF NOT EXISTS used_general_knowledge BOOLEAN DEFAULT false;

-- Add index for used_general_knowledge to help filter queries that used general knowledge
CREATE INDEX IF NOT EXISTS idx_unanswered_queries_used_general_knowledge
ON unanswered_queries(used_general_knowledge);
