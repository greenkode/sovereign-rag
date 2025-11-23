INSERT INTO identity.oauth_users (username, password, email, enabled)
VALUES 
    ('testuser', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'testuser@example.com', true),
    ('john.doe', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'john.doe@example.com', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO identity.oauth_user_authorities (user_id, authority)
SELECT id, 'ROLE_USER' FROM identity.oauth_users WHERE username IN ('testuser', 'john.doe')
ON CONFLICT DO NOTHING;