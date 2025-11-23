CREATE TABLE IF NOT EXISTS trusted_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_fingerprint_hash VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    trusted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trust_count INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trusted_device_user FOREIGN KEY (user_id) REFERENCES oauth_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_trusted_devices_user_id ON trusted_devices(user_id);
CREATE INDEX idx_trusted_devices_fingerprint_hash ON trusted_devices(device_fingerprint_hash);
CREATE INDEX idx_trusted_devices_expires_at ON trusted_devices(expires_at);
CREATE UNIQUE INDEX idx_trusted_devices_user_fingerprint ON trusted_devices(user_id, device_fingerprint_hash);

COMMENT ON TABLE trusted_devices IS 'Stores trusted devices for users to skip 2FA';
COMMENT ON COLUMN trusted_devices.device_fingerprint IS 'Original device fingerprint for display purposes';
COMMENT ON COLUMN trusted_devices.device_fingerprint_hash IS 'SHA-256 hash of device fingerprint for secure comparison';
COMMENT ON COLUMN trusted_devices.trust_count IS 'Number of times this device has been trusted';