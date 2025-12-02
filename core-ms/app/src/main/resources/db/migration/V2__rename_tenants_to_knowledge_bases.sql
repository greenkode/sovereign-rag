ALTER TABLE IF EXISTS tenants RENAME TO knowledge_bases;

ALTER TABLE knowledge_bases RENAME COLUMN database_name TO schema_name;

ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS oauth_client_id VARCHAR(100);
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS description TEXT;

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_organization_id ON knowledge_bases(organization_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_oauth_client_id ON knowledge_bases(oauth_client_id);

DROP INDEX IF EXISTS idx_tenants_status;
DROP INDEX IF EXISTS idx_tenants_client_id;
DROP INDEX IF EXISTS idx_tenants_database_name;

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_status ON knowledge_bases(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_schema_name ON knowledge_bases(schema_name);
