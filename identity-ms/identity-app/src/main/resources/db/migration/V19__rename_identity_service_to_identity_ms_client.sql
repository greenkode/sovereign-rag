UPDATE identity.oauth_registered_clients
SET client_id = 'identity-ms-client',
    client_name = 'Identity MS Client',
    updated_at = CURRENT_TIMESTAMP
WHERE client_id = 'identity-service';