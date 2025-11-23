INSERT INTO identity.oauth_registered_clients (
    id,
    client_id,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
) VALUES (
    gen_random_uuid()::text,
    'core-ms-client',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Core MS Client',
    'client_secret_basic,client_secret_post',
    'client_credentials,refresh_token',
    NULL,
    NULL,
    'openid,profile,read,write',
    '{"requireAuthorizationConsent":false,"requireProofKey":false}',
    '{"accessTokenTimeToLive":"PT30M","refreshTokenTimeToLive":"PT12H","reuseRefreshTokens":false}'
) ON CONFLICT (client_id) DO UPDATE SET
    client_secret = EXCLUDED.client_secret,
    client_name = EXCLUDED.client_name,
    client_authentication_methods = EXCLUDED.client_authentication_methods,
    authorization_grant_types = EXCLUDED.authorization_grant_types,
    scopes = EXCLUDED.scopes,
    client_settings = EXCLUDED.client_settings,
    token_settings = EXCLUDED.token_settings,
    updated_at = CURRENT_TIMESTAMP;