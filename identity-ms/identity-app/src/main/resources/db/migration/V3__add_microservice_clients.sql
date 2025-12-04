INSERT INTO oauth_scope (name) VALUES
    ('internal')
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_registered_client (
    id, client_id, client_id_issued_at, client_secret, client_name,
    failed_auth_attempts, environment_mode, status, plan, version, created_at
) VALUES
    ('core-ms', 'core-ms', NOW(),
     '$2a$10$UcQHI9SVJfyvt0UuuhnCteEuLSve1rq1vPQZc.hRCJKzgUH5lpoja',
     'Core MS Service Client', 0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()),
    ('audit-ms', 'audit-ms', NOW(),
     '$2a$10$UcQHI9SVJfyvt0UuuhnCteEuLSve1rq1vPQZc.hRCJKzgUH5lpoja',
     'Audit MS Service Client', 0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()),
    ('notification-ms', 'notification-ms', NOW(),
     '$2a$10$UcQHI9SVJfyvt0UuuhnCteEuLSve1rq1vPQZc.hRCJKzgUH5lpoja',
     'Notification MS Service Client', 0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()),
    ('ingestion-ms', 'ingestion-ms', NOW(),
     '$2a$10$UcQHI9SVJfyvt0UuuhnCteEuLSve1rq1vPQZc.hRCJKzgUH5lpoja',
     'Ingestion MS Service Client', 0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()),
    ('license-ms', 'license-ms', NOW(),
     '$2a$10$UcQHI9SVJfyvt0UuuhnCteEuLSve1rq1vPQZc.hRCJKzgUH5lpoja',
     'License MS Service Client', 0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW())
ON CONFLICT (client_id) DO NOTHING;

INSERT INTO oauth_client_scope (client_id, scope_id)
SELECT t.client_id, s.id FROM (
    VALUES
        ('core-ms', 'internal'),
        ('audit-ms', 'internal'),
        ('notification-ms', 'internal'),
        ('ingestion-ms', 'internal'),
        ('license-ms', 'internal')
) AS t(client_id, scope_name)
JOIN oauth_scope s ON s.name = t.scope_name
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_authentication_method (client_id, method_id)
SELECT t.client_id, m.id FROM (
    VALUES ('core-ms'), ('audit-ms'), ('notification-ms'), ('ingestion-ms'), ('license-ms')
) AS t(client_id)
CROSS JOIN oauth_authentication_method m
WHERE m.name = 'client_secret_basic'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_grant_type (client_id, grant_type_id)
SELECT t.client_id, g.id FROM (
    VALUES ('core-ms'), ('audit-ms'), ('notification-ms'), ('ingestion-ms'), ('license-ms')
) AS t(client_id)
CROSS JOIN oauth_grant_type g
WHERE g.name = 'client_credentials'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_token_setting (client_id, setting_name, setting_value)
SELECT t.client_id, t.setting_name, t.setting_value FROM (
    VALUES
        ('core-ms', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
        ('core-ms', 'REUSE_REFRESH_TOKENS', 'false'),
        ('audit-ms', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
        ('audit-ms', 'REUSE_REFRESH_TOKENS', 'false'),
        ('notification-ms', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
        ('notification-ms', 'REUSE_REFRESH_TOKENS', 'false'),
        ('ingestion-ms', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
        ('ingestion-ms', 'REUSE_REFRESH_TOKENS', 'false'),
        ('license-ms', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
        ('license-ms', 'REUSE_REFRESH_TOKENS', 'false')
) AS t(client_id, setting_name, setting_value)
ON CONFLICT DO NOTHING;
