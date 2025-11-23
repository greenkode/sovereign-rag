-- Update core-ms-client with correct BCrypt hash for "password"
-- This hash was generated using BCryptPasswordEncoder with strength 10
UPDATE identity.oauth_registered_clients 
SET client_secret = '$2a$10$wXHJZx4r5OwOQvZvNx5YCOmEk0F9JRC7aUk/0Y2KfP7VfKiEm0wXi'
WHERE client_id = 'core-ms-client';

-- Log what we're updating
DO $$
BEGIN
    RAISE NOTICE 'Updated core-ms-client password to BCrypt hash of "password"';
END $$;