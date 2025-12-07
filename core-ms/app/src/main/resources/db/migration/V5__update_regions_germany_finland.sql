UPDATE region SET enabled = FALSE WHERE code NOT IN ('eu-west-1');

INSERT INTO region (code, name, continent, city, country, country_code, flag, enabled, sort_order)
VALUES ('eu-north-1', 'Europe (Helsinki)', 'eu', 'Helsinki', 'Finland', 'FI', '🇫🇮', TRUE, 2)
ON CONFLICT (code) DO UPDATE SET enabled = TRUE, sort_order = 2;

UPDATE region SET sort_order = 1, enabled = TRUE WHERE code = 'eu-west-1';
