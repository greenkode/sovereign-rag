INSERT INTO oauth_scopes (name) VALUES
    ('openid'),
    ('profile'),
    ('email'),
    ('read'),
    ('write'),
    ('merchant')
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_authentication_methods (name) VALUES
    ('client_secret_basic'),
    ('client_secret_post')
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_grant_types (name) VALUES
    ('client_credentials'),
    ('refresh_token'),
    ('authorization_code')
ON CONFLICT (name) DO NOTHING;
