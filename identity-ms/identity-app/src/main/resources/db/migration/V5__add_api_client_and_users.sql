-- Add API client with password grant support
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
    'api-client',
    '$2a$10$RG0P7hqXz3Ee0JqQPBnZjOZ5V9wDt/7LbqD5vwZX8KhETSUY0C8fO',
    'API Client',
    'client_secret_basic,client_secret_post',
    'password,refresh_token,client_credentials',
    NULL,
    NULL,
    'openid,profile,email,read,write,billpay',
    '{"requireAuthorizationConsent":false,"requireProofKey":false}',
    '{"accessTokenTimeToLive":"PT1H","refreshTokenTimeToLive":"P30D","reuseRefreshTokens":false}'
) ON CONFLICT (client_id) DO UPDATE SET
    client_secret = EXCLUDED.client_secret,
    client_name = EXCLUDED.client_name,
    client_authentication_methods = EXCLUDED.client_authentication_methods,
    authorization_grant_types = EXCLUDED.authorization_grant_types,
    scopes = EXCLUDED.scopes,
    client_settings = EXCLUDED.client_settings,
    token_settings = EXCLUDED.token_settings,
    updated_at = CURRENT_TIMESTAMP;

-- Add API users with appropriate roles
INSERT INTO identity.oauth_users (username, password, email, enabled)
VALUES 
    ('api_user', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'api_user@example.com', true),
    ('billpay_api', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'billpay@example.com', true)
ON CONFLICT (username) DO NOTHING;

-- Grant API roles
INSERT INTO identity.oauth_user_authorities (user_id, authority)
SELECT id, 'ROLE_API' FROM identity.oauth_users WHERE username IN ('api_user', 'billpay_api')
ON CONFLICT DO NOTHING;

-- Grant billpay specific role
INSERT INTO identity.oauth_user_authorities (user_id, authority)
SELECT id, 'ROLE_BILLPAY' FROM identity.oauth_users WHERE username = 'billpay_api'
ON CONFLICT DO NOTHING;

-- Also add a mobile app client for future use
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
    'mobile-app',
    '$2a$10$RG0P7hqXz3Ee0JqQPBnZjOZ5V9wDt/7LbqD5vwZX8KhETSUY0C8fO',
    'Mobile Application',
    'client_secret_post',
    'password,refresh_token',
    'myapp://callback',
    'myapp://logout',
    'openid,profile,email,read,write,billpay',
    '{"requireAuthorizationConsent":false,"requireProofKey":false}',
    '{"accessTokenTimeToLive":"PT30M","refreshTokenTimeToLive":"P30D","reuseRefreshTokens":false}'
) ON CONFLICT (client_id) DO UPDATE SET
    client_secret = EXCLUDED.client_secret,
    client_name = EXCLUDED.client_name,
    client_authentication_methods = EXCLUDED.client_authentication_methods,
    authorization_grant_types = EXCLUDED.authorization_grant_types,
    redirect_uris = EXCLUDED.redirect_uris,
    post_logout_redirect_uris = EXCLUDED.post_logout_redirect_uris,
    scopes = EXCLUDED.scopes,
    client_settings = EXCLUDED.client_settings,
    token_settings = EXCLUDED.token_settings,
    updated_at = CURRENT_TIMESTAMP;