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
    'react-frontend',
    NULL,
    'React Frontend Application',
    'none',
    'authorization_code,refresh_token',
    'http://localhost:3000/callback,http://localhost:3001/callback,https://localhost:3000/callback,http://localhost:3000/auth/callback,http://localhost:3001/auth/callback,https://localhost:3000/auth/callback',
    'http://localhost:3000,http://localhost:3001,https://localhost:3000',
    'openid,profile,email,read,write',
    '{"requireAuthorizationConsent":false,"requireProofKey":true}',
    '{"accessTokenTimeToLive":"PT15M","refreshTokenTimeToLive":"P7D","reuseRefreshTokens":false}'
) ON CONFLICT (client_id) DO UPDATE SET
    client_name = EXCLUDED.client_name,
    client_authentication_methods = EXCLUDED.client_authentication_methods,
    authorization_grant_types = EXCLUDED.authorization_grant_types,
    redirect_uris = EXCLUDED.redirect_uris,
    post_logout_redirect_uris = EXCLUDED.post_logout_redirect_uris,
    scopes = EXCLUDED.scopes,
    client_settings = EXCLUDED.client_settings,
    token_settings = EXCLUDED.token_settings,
    updated_at = CURRENT_TIMESTAMP;