-- Update core-ms client with a simple password for debugging
UPDATE identity.oauth_registered_clients 
SET client_secret = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' -- "password"
WHERE client_id = 'core-ms-client';

-- Also ensure the client exists with correct settings
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
    token_settings,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid()::text,
    'core-ms-client',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- "password"
    'Core MS Client',
    'client_secret_basic,client_secret_post',
    'client_credentials,refresh_token',
    NULL,
    NULL,
    'openid,profile,read,write',
    '{"requireAuthorizationConsent":false,"requireProofKey":false}',
    '{"accessTokenTimeToLive":"PT30M","refreshTokenTimeToLive":"PT12H","reuseRefreshTokens":false}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (client_id) DO UPDATE SET
    client_secret = EXCLUDED.client_secret,
    client_authentication_methods = EXCLUDED.client_authentication_methods,
    authorization_grant_types = EXCLUDED.authorization_grant_types,
    scopes = EXCLUDED.scopes,
    updated_at = CURRENT_TIMESTAMP;