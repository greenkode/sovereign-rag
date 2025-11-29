CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE IF NOT EXISTS identity.oauth_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    middle_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    merchant_id UUID,
    user_type VARCHAR(20),
    trust_level VARCHAR(20),
    email_verified BOOLEAN NOT NULL DEFAULT false,
    phone_number_verified BOOLEAN NOT NULL DEFAULT false,
    invitation_status BOOLEAN NOT NULL DEFAULT false,
    date_of_birth DATE,
    tax_identification_number VARCHAR(50),
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_login TIMESTAMPTZ,
    environment_preference VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    environment_last_switched_at TIMESTAMPTZ,
    registration_source VARCHAR(50) DEFAULT 'INVITATION',
    picture_url VARCHAR(1024),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS identity.oauth_user_authorities (
    user_id UUID NOT NULL,
    authority VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, authority),
    CONSTRAINT fk_user_authorities_user FOREIGN KEY (user_id) REFERENCES identity.oauth_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_provider_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_oauth_provider_user FOREIGN KEY (user_id) REFERENCES identity.oauth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id)
);

CREATE TABLE IF NOT EXISTS identity.oauth_registered_clients (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    client_secret VARCHAR(255),
    client_secret_expires_at TIMESTAMPTZ,
    sandbox_client_secret VARCHAR(255),
    sandbox_client_secret_expires_at TIMESTAMPTZ,
    production_client_secret VARCHAR(255),
    production_client_secret_expires_at TIMESTAMPTZ,
    client_name VARCHAR(200) NOT NULL,
    failed_auth_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_auth TIMESTAMPTZ,
    environment_mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    domain VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    plan VARCHAR(50) DEFAULT 'TRIAL',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS identity.oauth_scopes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS identity.oauth_authentication_methods (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS identity.oauth_grant_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_scopes (
    client_id VARCHAR(100) NOT NULL,
    scope_id INT NOT NULL,
    PRIMARY KEY (client_id, scope_id),
    CONSTRAINT fk_client_scope_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_scope_scope FOREIGN KEY (scope_id) REFERENCES identity.oauth_scopes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_authentication_methods (
    client_id VARCHAR(100) NOT NULL,
    method_id INT NOT NULL,
    PRIMARY KEY (client_id, method_id),
    CONSTRAINT fk_client_method_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_method_method FOREIGN KEY (method_id) REFERENCES identity.oauth_authentication_methods(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_grant_types (
    client_id VARCHAR(100) NOT NULL,
    grant_type_id INT NOT NULL,
    PRIMARY KEY (client_id, grant_type_id),
    CONSTRAINT fk_client_grant_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_grant_type FOREIGN KEY (grant_type_id) REFERENCES identity.oauth_grant_types(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_redirect_uris (
    client_id VARCHAR(100) NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (client_id, uri),
    CONSTRAINT fk_redirect_uri_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_post_logout_redirect_uris (
    client_id VARCHAR(100) NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (client_id, uri),
    CONSTRAINT fk_post_logout_uri_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_settings (
    client_id VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (client_id, setting_name),
    CONSTRAINT fk_client_setting_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.oauth_client_token_settings (
    client_id VARCHAR(100) NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (client_id, setting_name),
    CONSTRAINT fk_token_setting_client FOREIGN KEY (client_id) REFERENCES identity.oauth_registered_clients(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jti VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    token_hash VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_jti VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES identity.oauth_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.trusted_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_fingerprint_hash VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    trusted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trust_count INTEGER DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_trusted_device_user FOREIGN KEY (user_id) REFERENCES identity.oauth_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity.process (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    expiry TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    external_reference VARCHAR(255),
    channel VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT unique_process_type_external_reference UNIQUE (type, external_reference)
);

CREATE TABLE IF NOT EXISTS identity.process_request (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_request_process FOREIGN KEY (process_id) REFERENCES identity.process (id)
);

CREATE TABLE IF NOT EXISTS identity.process_request_data (
    process_request_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    PRIMARY KEY (process_request_id, name),
    CONSTRAINT fk_process_request_data FOREIGN KEY (process_request_id) REFERENCES identity.process_request (id)
);

CREATE TABLE IF NOT EXISTS identity.process_request_stakeholder (
    id BIGSERIAL PRIMARY KEY,
    process_request_id BIGINT NOT NULL,
    stakeholder_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_request_stakeholder FOREIGN KEY (process_request_id) REFERENCES identity.process_request (id)
);

CREATE TABLE IF NOT EXISTS identity.process_event_transition (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL,
    event VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    old_state VARCHAR(255) NOT NULL,
    new_state VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_event_transition FOREIGN KEY (process_id) REFERENCES identity.process (id)
);

CREATE TABLE IF NOT EXISTS identity.rate_limit_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_name VARCHAR(100) NOT NULL,
    subscription_tier VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    capacity INT NOT NULL,
    time_value INT NOT NULL,
    time_unit VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_by VARCHAR(255) DEFAULT 'system',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT rate_limit_config_method_tier_scope_key UNIQUE (method_name, subscription_tier, scope)
);

CREATE TABLE IF NOT EXISTS identity.country (
    id SERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    iso2_code VARCHAR(2) NOT NULL,
    iso3_code VARCHAR(3) NOT NULL,
    numeric_code VARCHAR(3) NOT NULL,
    dial_code VARCHAR(10) NOT NULL,
    flag_url VARCHAR(255) NOT NULL,
    region VARCHAR(50) DEFAULT '',
    sub_region VARCHAR(100) DEFAULT '',
    enabled BOOLEAN DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) DEFAULT 'system',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(iso2_code),
    UNIQUE(iso3_code),
    UNIQUE(public_id)
);

CREATE INDEX IF NOT EXISTS idx_oauth_users_username ON identity.oauth_users(username);
CREATE INDEX IF NOT EXISTS idx_oauth_users_lockout ON identity.oauth_users (username, locked_until, failed_login_attempts);
CREATE INDEX IF NOT EXISTS idx_oauth_users_registration_source ON identity.oauth_users(registration_source);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_clients_client_id ON identity.oauth_registered_clients(client_id);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_clients_lockout ON identity.oauth_registered_clients (client_id, locked_until, failed_auth_attempts);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_registered_clients_domain ON identity.oauth_registered_clients(domain);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_clients_status ON identity.oauth_registered_clients(status);
CREATE INDEX IF NOT EXISTS idx_oauth_provider_user_id ON identity.oauth_provider_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_provider_email ON identity.oauth_provider_accounts(provider_email);
CREATE INDEX IF NOT EXISTS idx_oauth_provider ON identity.oauth_provider_accounts(provider, provider_user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_jti ON identity.refresh_tokens(jti);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON identity.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON identity.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_trusted_devices_user_id ON identity.trusted_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_trusted_devices_fingerprint_hash ON identity.trusted_devices(device_fingerprint_hash);
CREATE INDEX IF NOT EXISTS idx_trusted_devices_expires_at ON identity.trusted_devices(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_trusted_devices_user_fingerprint ON identity.trusted_devices(user_id, device_fingerprint_hash);
CREATE INDEX IF NOT EXISTS idx_rate_limit_method_tier_scope ON identity.rate_limit_config(method_name, subscription_tier, scope, active);
CREATE INDEX IF NOT EXISTS idx_country_enabled ON identity.country(enabled);
CREATE INDEX IF NOT EXISTS idx_country_iso2_code ON identity.country(iso2_code);
CREATE INDEX IF NOT EXISTS idx_country_name ON identity.country(name);
