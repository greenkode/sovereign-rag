DROP INDEX IF EXISTS idx_oauth_registered_client_client_id;
DROP INDEX IF EXISTS idx_oauth_registered_client_lockout;
DROP INDEX IF EXISTS idx_oauth_registered_client_domain;
DROP INDEX IF EXISTS idx_oauth_registered_client_status;
DROP INDEX IF EXISTS idx_oauth_client_org_id;
DROP INDEX IF EXISTS idx_oauth_client_kb_id;
DROP INDEX IF EXISTS idx_oauth_client_kb_id_unique;

ALTER TABLE oauth_client_scope DROP CONSTRAINT IF EXISTS fk_client_scope_client;
ALTER TABLE oauth_client_authentication_method DROP CONSTRAINT IF EXISTS fk_client_method_client;
ALTER TABLE oauth_client_grant_type DROP CONSTRAINT IF EXISTS fk_client_grant_client;
ALTER TABLE oauth_client_redirect_uri DROP CONSTRAINT IF EXISTS fk_redirect_uri_client;
ALTER TABLE oauth_client_post_logout_redirect_uri DROP CONSTRAINT IF EXISTS fk_post_logout_uri_client;
ALTER TABLE oauth_client_setting DROP CONSTRAINT IF EXISTS fk_client_setting_client;
ALTER TABLE oauth_client_token_setting DROP CONSTRAINT IF EXISTS fk_token_setting_client;

UPDATE oauth_client_scope SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_scope SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_scope SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_scope SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_scope SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_scope SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_authentication_method SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_authentication_method SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_authentication_method SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_authentication_method SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_authentication_method SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_authentication_method SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_grant_type SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_grant_type SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_grant_type SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_grant_type SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_grant_type SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_grant_type SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_token_setting SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_token_setting SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_token_setting SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_token_setting SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_token_setting SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_token_setting SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_setting SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_setting SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_setting SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_setting SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_setting SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_setting SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_redirect_uri SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_redirect_uri SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_redirect_uri SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_redirect_uri SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_redirect_uri SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_redirect_uri SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE client_id = 'identity-ms-client';
UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE client_id = 'core-ms';
UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE client_id = 'audit-ms';
UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE client_id = 'notification-ms';
UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE client_id = 'ingestion-ms';
UPDATE oauth_client_post_logout_redirect_uri SET client_id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE client_id = 'license-ms';

UPDATE oauth_registered_client SET id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE id = 'identity-ms-client';
UPDATE oauth_registered_client SET id = 'a1b2c3d4-5e6f-7890-abcd-ef1234567890' WHERE id = 'core-ms';
UPDATE oauth_registered_client SET id = 'b2c3d4e5-6f78-90ab-cdef-123456789012' WHERE id = 'audit-ms';
UPDATE oauth_registered_client SET id = 'c3d4e5f6-7890-abcd-ef12-345678901234' WHERE id = 'notification-ms';
UPDATE oauth_registered_client SET id = 'd4e5f6a7-890a-bcde-f123-456789012345' WHERE id = 'ingestion-ms';
UPDATE oauth_registered_client SET id = 'e5f6a7b8-90ab-cdef-1234-567890123456' WHERE id = 'license-ms';

ALTER TABLE oauth_client_scope ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_authentication_method ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_grant_type ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_redirect_uri ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_post_logout_redirect_uri ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_setting ALTER COLUMN client_id TYPE UUID USING client_id::uuid;
ALTER TABLE oauth_client_token_setting ALTER COLUMN client_id TYPE UUID USING client_id::uuid;

ALTER TABLE oauth_registered_client ALTER COLUMN id TYPE UUID USING id::uuid;

ALTER TABLE oauth_client_scope
    ADD CONSTRAINT fk_client_scope_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_authentication_method
    ADD CONSTRAINT fk_client_method_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_grant_type
    ADD CONSTRAINT fk_client_grant_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_redirect_uri
    ADD CONSTRAINT fk_redirect_uri_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_post_logout_redirect_uri
    ADD CONSTRAINT fk_post_logout_uri_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_setting
    ADD CONSTRAINT fk_client_setting_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;
ALTER TABLE oauth_client_token_setting
    ADD CONSTRAINT fk_token_setting_client FOREIGN KEY (client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_client_id ON oauth_registered_client(client_id);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_lockout ON oauth_registered_client (client_id, locked_until, failed_auth_attempts);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_registered_client_domain ON oauth_registered_client(domain);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_status ON oauth_registered_client(status);
CREATE INDEX IF NOT EXISTS idx_oauth_client_org_id ON oauth_registered_client(organization_id);
CREATE INDEX IF NOT EXISTS idx_oauth_client_kb_id ON oauth_registered_client(knowledge_base_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_client_kb_id_unique ON oauth_registered_client(knowledge_base_id) WHERE knowledge_base_id IS NOT NULL;
