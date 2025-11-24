-- Audit MS Schema Initialization
-- Audit Log Table

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id VARCHAR(255) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    identity_type VARCHAR(255) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    event VARCHAR(255) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    time_recorded TIMESTAMP NOT NULL,
    payload TEXT NOT NULL,
    ip_address VARCHAR(45) NOT NULL DEFAULT 'N/A'
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_merchant_id ON audit_log(merchant_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource ON audit_log(resource);
CREATE INDEX IF NOT EXISTS idx_audit_log_event ON audit_log(event);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_time ON audit_log(event_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_time_recorded ON audit_log(time_recorded);
