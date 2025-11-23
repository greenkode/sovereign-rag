ALTER TABLE identity.oauth_registered_clients
    ADD COLUMN sandbox_client_secret VARCHAR(255),
    ADD COLUMN sandbox_client_secret_expires_at TIMESTAMP,
    ADD COLUMN production_client_secret VARCHAR(255),
    ADD COLUMN production_client_secret_expires_at TIMESTAMP;

UPDATE identity.oauth_registered_clients
SET production_client_secret = client_secret,
    production_client_secret_expires_at = client_secret_expires_at
WHERE client_secret IS NOT NULL;
