#!/bin/bash
set -e

echo "Initializing SovereignRAG database schemas..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create schemas for multi-tenant architecture
    CREATE SCHEMA IF NOT EXISTS master;
    CREATE SCHEMA IF NOT EXISTS core;
    CREATE SCHEMA IF NOT EXISTS identity;
    CREATE SCHEMA IF NOT EXISTS audit;

    -- Grant permissions
    GRANT ALL PRIVILEGES ON SCHEMA master TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA core TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA identity TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON SCHEMA audit TO $POSTGRES_USER;

    -- Set default search path
    ALTER DATABASE $POSTGRES_DB SET search_path TO master, core, identity, audit, public;

    -- Create extension for UUID generation
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";

    COMMENT ON SCHEMA master IS 'Master schema for tenant metadata and configuration';
    COMMENT ON SCHEMA core IS 'Core RAG application schema';
    COMMENT ON SCHEMA identity IS 'Identity and authentication schema';
    COMMENT ON SCHEMA audit IS 'Audit logging schema';

    EOSQL

echo "Database schemas initialized successfully!"
