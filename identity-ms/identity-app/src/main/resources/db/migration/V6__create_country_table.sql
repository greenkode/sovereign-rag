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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(iso2_code),
    UNIQUE(iso3_code),
    UNIQUE(public_id)
);

CREATE INDEX IF NOT EXISTS idx_country_enabled ON identity.country(enabled);
CREATE INDEX IF NOT EXISTS idx_country_iso2_code ON identity.country(iso2_code);
CREATE INDEX IF NOT EXISTS idx_country_name ON identity.country(name);
