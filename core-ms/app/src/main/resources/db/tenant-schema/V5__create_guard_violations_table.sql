-- Guard violations table for monitoring and security analysis
-- Stores all guardrail violations to detect patterns and security threats

CREATE TABLE guard_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Tenant and session information
    tenant_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    user_id VARCHAR(255),

    -- Guard information
    guard_type VARCHAR(100) NOT NULL,  -- 'INPUT' or 'OUTPUT'
    guard_name VARCHAR(255) NOT NULL,  -- Class name of the guardrail (e.g., 'PromptInjectionGuardrail')

    -- Violation details
    violation_reason TEXT NOT NULL,     -- Why the guard blocked the request
    user_message TEXT,                  -- The message that triggered the violation (may be truncated or redacted)
    ai_response TEXT,                   -- The AI response that triggered the violation (for OUTPUT guards)
    severity VARCHAR(50) NOT NULL,      -- 'INFO', 'WARNING', 'ERROR', 'CRITICAL'

    -- Pattern matched (for analysis)
    matched_pattern VARCHAR(500),       -- The specific pattern that was matched

    -- Metadata (JSON for additional context)
    metadata JSONB,                     -- Additional context (redaction count, confidence scores, etc.)

    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_guard_violations_tenant_created ON guard_violations(tenant_id, created_at DESC);
CREATE INDEX idx_guard_violations_guard_name ON guard_violations(guard_name);
CREATE INDEX idx_guard_violations_severity ON guard_violations(severity);
CREATE INDEX idx_guard_violations_session ON guard_violations(session_id);
CREATE INDEX idx_guard_violations_guard_type ON guard_violations(guard_type);

-- Comment
COMMENT ON TABLE guard_violations IS 'Logs all guardrail violations for security monitoring, pattern analysis, and threat detection';
COMMENT ON COLUMN guard_violations.guard_type IS 'INPUT for user message validation, OUTPUT for AI response validation';
COMMENT ON COLUMN guard_violations.severity IS 'Severity level: INFO (informational), WARNING (potential issue), ERROR (violation blocked), CRITICAL (security incident)';
COMMENT ON COLUMN guard_violations.matched_pattern IS 'The specific pattern that triggered the violation (for security analysis)';
