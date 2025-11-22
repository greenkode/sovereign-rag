# Phase 1: Database Schema Setup

This guide walks you through setting up the PostgreSQL database schemas for the multi-tenant architecture.

## Prerequisites

- PostgreSQL 16+ installed and running
- pgvector extension installed
- Database credentials: `sovereignrag:RespectTheHangover`
- Existing database: `sovereignrag_ai`

## Step 1: Install pgvector Extension

If you haven't installed pgvector yet:

```bash
# On macOS with Homebrew
brew install pgvector

# Or follow instructions at: https://github.com/pgvector/pgvector
```

## Step 2: Create Master Database

The master database stores tenant registry and system-wide metadata.

### Option A: Using psql Command Line

```bash
# Connect to PostgreSQL
psql -h localhost -U sovereignrag -d postgres

# Create master database
CREATE DATABASE sovereignrag_master
    WITH OWNER = sovereignrag
         ENCODING = 'UTF8'
         LC_COLLATE = 'en_US.UTF-8'
         LC_CTYPE = 'en_US.UTF-8'
         TEMPLATE = template0;

# Exit and apply schema
\q

# Apply the schema
psql -h localhost -U sovereignrag -d sovereignrag_master -f setup-master-database.sql
```

### Option B: Using IntelliJ Database Console

1. Open IntelliJ IDEA
2. Open Database tool window (View → Tool Windows → Database)
3. Connect to your PostgreSQL instance
4. Right-click and select "New → Database"
   - Name: `sovereignrag_master`
   - Owner: `sovereignrag`
5. Open `setup-master-database.sql` in IntelliJ
6. Select the `sovereignrag_master` database in the dropdown
7. Execute the script (Ctrl+Enter or click Run)

### Option C: Using pgAdmin

1. Open pgAdmin
2. Right-click on "Databases" → "Create" → "Database"
   - Name: `sovereignrag_master`
   - Owner: `sovereignrag`
3. Open Query Tool for `sovereignrag_master`
4. Copy and paste contents of `setup-master-database.sql`
5. Execute the script

## Step 3: Verify Master Database Setup

Connect to the master database and verify tables were created:

```sql
\c sovereignrag_master

-- List all schemas
\dn

-- Should show: master schema

-- List tables in master schema
\dt master.*

-- Should show:
-- master.api_keys
-- master.audit_log
-- master.tenant_usage
-- master.tenants
```

## Step 4: Test Tenant Schema (Optional)

You can test the tenant schema by creating a test tenant database:

```bash
# Create test tenant database
psql -h localhost -U sovereignrag -d postgres -c "CREATE DATABASE sovereignrag_tenant_test"

# Connect and enable extensions
psql -h localhost -U sovereignrag -d sovereignrag_tenant_test <<EOF
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
EOF

# Apply tenant schema
psql -h localhost -U sovereignrag -d sovereignrag_tenant_test -f core-ms/core-ai/src/main/resources/db/tenant-schema/V1__create_tenant_schema.sql
```

Verify tenant database:

```sql
\c sovereignrag_tenant_test

-- List all tables
\dt

-- Should show:
-- chat_messages
-- chat_sessions
-- document_segments
-- documents
-- escalations
-- feedback
-- unanswered_queries
```

## Schema Overview

### Master Database (sovereignrag_master)

**Purpose**: Store tenant registry and system-wide data

**Schema**: `master` (dedicated schema for clean organization)

**Tables**:
- `master.tenants` - Registry of all WordPress sites using the plugin
- `master.tenant_usage` - Daily usage metrics per tenant
- `master.api_keys` - API keys for authentication (multiple per tenant)
- `master.audit_log` - Audit trail of all tenant actions

**Why a dedicated schema?**
- Keeps application tables separate from PostgreSQL system objects
- Better security (can control schema-level permissions)
- Clearer architecture and namespace management
- Professional best practice

### Tenant Databases (sovereignrag_tenant_*)

**Purpose**: Store content and chat data for each tenant (physically isolated)

**Schema**: `public` (since each database is already isolated)

**Tables**:
- `documents` - Main content with vector embeddings
- `document_segments` - Chunked content (optional)
- `chat_sessions` - Chat conversation sessions
- `chat_messages` - Individual chat messages
- `escalations` - Customer support escalation requests
- `unanswered_queries` - Questions that couldn't be answered
- `feedback` - User ratings and feedback

## Directory Structure

```
sovereign-rag/
├── setup-master-database.sql              # Master DB setup script
├── MIGRATION_PLAN.md                      # Full migration plan
├── PHASE1_SETUP.md                        # This file
└── core-ms/
    └── core-ai/
        └── src/
            └── main/
                └── resources/
                    └── db/
                        ├── master-schema/
                        │   └── V1__create_master_schema.sql
                        └── tenant-schema/
                            └── V1__create_tenant_schema.sql
```

## Next Steps

After completing Phase 1:

1. **Phase 2**: Set up Kotlin domain models and tenant context
2. **Phase 3**: Implement tenant registry service
3. **Phase 4**: Migrate ContentService to pgvector

See `MIGRATION_PLAN.md` for the complete roadmap.

## Troubleshooting

### Error: "extension vector does not exist"

Install pgvector:
```bash
# macOS
brew install pgvector

# Linux (Ubuntu/Debian)
sudo apt-get install postgresql-16-pgvector

# Then restart PostgreSQL
brew services restart postgresql@16
# or
sudo systemctl restart postgresql
```

### Error: "database already exists"

If `sovereignrag_master` already exists:
```sql
-- Drop and recreate (WARNING: destroys all data)
DROP DATABASE sovereignrag_master;
CREATE DATABASE sovereignrag_master;

-- Or connect to existing and run schema
\c sovereignrag_master
-- Then run the schema script
```

### Error: "permission denied"

Make sure you're connected as the `sovereignrag` user or a superuser:
```bash
psql -h localhost -U sovereignrag -d postgres
```

## Validation Queries

Run these to ensure everything is set up correctly:

```sql
-- Connect to master database
\c sovereignrag_master

-- Check schemas
\dn

-- Check master schema tables
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'master'
ORDER BY table_name;

-- Verify you can query master tables (search_path should include master)
SELECT COUNT(*) FROM master.tenants;

-- Check tenant database tables (if test database created)
\c sovereignrag_tenant_test

SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Verify pgvector extension in tenant database
SELECT * FROM pg_extension WHERE extname = 'vector';
```

## Success Criteria

✅ Phase 1 is complete when:
- [ ] `sovereignrag_master` database exists
- [ ] `master` schema created within sovereignrag_master database
- [ ] All master schema tables created (master.tenants, master.tenant_usage, master.api_keys, master.audit_log)
- [ ] Tenant schema SQL files are ready in `db/tenant-schema/`
- [ ] pgvector extension is working
- [ ] (Optional) Test tenant database created and verified

## Application Configuration Notes

When configuring your Spring Boot application to connect to the master database:

```yaml
# For master database connection
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sovereignrag_master
    username: sovereignrag
    password: RespectTheHangover
    # Important: Set default schema to master
    hikari:
      schema: master
      connection-init-sql: SET search_path TO master, public
```

This ensures all queries default to the `master` schema without needing to prefix table names.

---

**Next**: Proceed to Phase 2 - Core Infrastructure (Kotlin domain models and tenant context)
