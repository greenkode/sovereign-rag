CREATE TABLE IF NOT EXISTS oauth_scopes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS oauth_authentication_methods (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS oauth_grant_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS oauth_client_scopes (
    client_id VARCHAR(100) NOT NULL,
    scope_id INT NOT NULL,
    PRIMARY KEY (client_id, scope_id),
    CONSTRAINT fk_client_scope_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_scope_scope FOREIGN KEY (scope_id) REFERENCES oauth_scopes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_authentication_methods (
    client_id VARCHAR(100) NOT NULL,
    method_id INT NOT NULL,
    PRIMARY KEY (client_id, method_id),
    CONSTRAINT fk_client_method_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_method_method FOREIGN KEY (method_id) REFERENCES oauth_authentication_methods(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_grant_types (
    client_id VARCHAR(100) NOT NULL,
    grant_type_id INT NOT NULL,
    PRIMARY KEY (client_id, grant_type_id),
    CONSTRAINT fk_client_grant_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_grant_type FOREIGN KEY (grant_type_id) REFERENCES oauth_grant_types(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_redirect_uris (
    client_id VARCHAR(100) NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (client_id, uri),
    CONSTRAINT fk_redirect_uri_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_post_logout_redirect_uris (
    client_id VARCHAR(100) NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (client_id, uri),
    CONSTRAINT fk_post_logout_uri_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_settings (
    client_id VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (client_id, setting_name),
    CONSTRAINT fk_client_setting_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_token_settings (
    client_id VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (client_id, setting_name),
    CONSTRAINT fk_token_setting_client FOREIGN KEY (client_id) REFERENCES oauth_registered_clients(id) ON DELETE CASCADE
);

INSERT INTO oauth_scopes (name)
SELECT DISTINCT unnest(string_to_array(scopes, ','))
FROM oauth_registered_clients
WHERE scopes IS NOT NULL AND scopes != ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_authentication_methods (name)
SELECT DISTINCT unnest(string_to_array(client_authentication_methods, ','))
FROM oauth_registered_clients
WHERE client_authentication_methods IS NOT NULL AND client_authentication_methods != ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_grant_types (name)
SELECT DISTINCT unnest(string_to_array(authorization_grant_types, ','))
FROM oauth_registered_clients
WHERE authorization_grant_types IS NOT NULL AND authorization_grant_types != ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_client_scopes (client_id, scope_id)
SELECT c.id, s.id
FROM oauth_registered_clients c
CROSS JOIN LATERAL unnest(string_to_array(c.scopes, ',')) AS scope_name
JOIN oauth_scopes s ON s.name = scope_name
WHERE c.scopes IS NOT NULL AND c.scopes != ''
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_authentication_methods (client_id, method_id)
SELECT c.id, m.id
FROM oauth_registered_clients c
CROSS JOIN LATERAL unnest(string_to_array(c.client_authentication_methods, ',')) AS method_name
JOIN oauth_authentication_methods m ON m.name = method_name
WHERE c.client_authentication_methods IS NOT NULL AND c.client_authentication_methods != ''
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_grant_types (client_id, grant_type_id)
SELECT c.id, g.id
FROM oauth_registered_clients c
CROSS JOIN LATERAL unnest(string_to_array(c.authorization_grant_types, ',')) AS grant_name
JOIN oauth_grant_types g ON g.name = grant_name
WHERE c.authorization_grant_types IS NOT NULL AND c.authorization_grant_types != ''
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_redirect_uris (client_id, uri)
SELECT id, unnest(string_to_array(redirect_uris, ','))
FROM oauth_registered_clients
WHERE redirect_uris IS NOT NULL AND redirect_uris != ''
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_post_logout_redirect_uris (client_id, uri)
SELECT id, unnest(string_to_array(post_logout_redirect_uris, ','))
FROM oauth_registered_clients
WHERE post_logout_redirect_uris IS NOT NULL AND post_logout_redirect_uris != ''
ON CONFLICT DO NOTHING;

ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS redirect_uris;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS post_logout_redirect_uris;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS scopes;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS client_authentication_methods;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS authorization_grant_types;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS client_settings;
ALTER TABLE oauth_registered_clients DROP COLUMN IF EXISTS token_settings;
