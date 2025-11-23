-- Update testuser password to 'password123'
-- BCrypt hash for 'password123': $2a$10$N.vF3KqhLzn2rsE5g5pX8eOF.3JQoZYLQJXZOzOe3XYV8sVqwqq8W
UPDATE identity.oauth_users 
SET password = '$2a$10$N.vF3KqhLzn2rsE5g5pX8eOF.3JQoZYLQJXZOzOe3XYV8sVqwqq8W',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'testuser';