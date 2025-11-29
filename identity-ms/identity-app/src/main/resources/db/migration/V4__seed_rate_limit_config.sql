INSERT INTO identity.rate_limit_config (method_name, subscription_tier, capacity, time_value, time_unit) VALUES
('login-attempts', 'TRIAL', 5, 1, 'MINUTES'),
('2fa-resend', 'TRIAL', 1, 1, 'MINUTES'),
('2fa-verify', 'TRIAL', 5, 1, 'MINUTES'),
('password-reset', 'TRIAL', 2, 5, 'MINUTES'),
('api-request', 'TRIAL', 100, 1, 'HOURS')
ON CONFLICT (method_name, subscription_tier) DO NOTHING;

INSERT INTO identity.rate_limit_config (method_name, subscription_tier, capacity, time_value, time_unit) VALUES
('login-attempts', 'STARTER', 10, 1, 'MINUTES'),
('2fa-resend', 'STARTER', 2, 1, 'MINUTES'),
('2fa-verify', 'STARTER', 10, 1, 'MINUTES'),
('password-reset', 'STARTER', 5, 5, 'MINUTES'),
('api-request', 'STARTER', 1000, 1, 'HOURS')
ON CONFLICT (method_name, subscription_tier) DO NOTHING;

INSERT INTO identity.rate_limit_config (method_name, subscription_tier, capacity, time_value, time_unit) VALUES
('login-attempts', 'PROFESSIONAL', 20, 1, 'MINUTES'),
('2fa-resend', 'PROFESSIONAL', 3, 1, 'MINUTES'),
('2fa-verify', 'PROFESSIONAL', 20, 1, 'MINUTES'),
('password-reset', 'PROFESSIONAL', 10, 5, 'MINUTES'),
('api-request', 'PROFESSIONAL', 5000, 1, 'HOURS')
ON CONFLICT (method_name, subscription_tier) DO NOTHING;

INSERT INTO identity.rate_limit_config (method_name, subscription_tier, capacity, time_value, time_unit) VALUES
('login-attempts', 'ENTERPRISE', 50, 1, 'MINUTES'),
('2fa-resend', 'ENTERPRISE', 5, 1, 'MINUTES'),
('2fa-verify', 'ENTERPRISE', 50, 1, 'MINUTES'),
('password-reset', 'ENTERPRISE', 20, 5, 'MINUTES'),
('api-request', 'ENTERPRISE', 50000, 1, 'HOURS')
ON CONFLICT (method_name, subscription_tier) DO NOTHING;
