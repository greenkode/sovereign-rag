-- Update core-ms-client with correct BCrypt hash for "password123"
-- This hash was generated using BCryptPasswordEncoder for the password "password123"
UPDATE identity.oauth_registered_clients 
SET client_secret = '$2a$12$uK1wINmUoMqplwydidlXjuo/NMclzbxbXyNaJI/Asd5WC/QAkSSAK'
WHERE client_id = 'core-ms-client';