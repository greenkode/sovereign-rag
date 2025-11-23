-- Update core-ms-client with provided BCrypt hash
UPDATE identity.oauth_registered_clients 
SET client_secret = '$2a$12$Ft2PrKbVJ70SA3nZ8zI71.Ylsej6FSK6NiVu85cHJcblCG5WFL.vW'
WHERE client_id = 'core-ms-client';