# Phase 2 Configuration Guide

## Application Properties

Add to `core-ms/app/src/main/resources/application.yml`:

```yaml
spring:
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/compilot_master
    username: compilot
    password: RespectTheHangover
    host: localhost
    port: 5432

    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create tables, use Flyway
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  # Flyway Configuration
  flyway:
    enabled: true
    locations: classpath:db/master-schema
    baseline-on-migrate: true
    baseline-version: 0

# Tenant Security (can be disabled for development)
compilot:
  tenant:
    security:
      enabled: true  # Set to false to disable authentication during development
```

## Development Mode (No Authentication)

To run without tenant authentication during development:

```yaml
compilot:
  tenant:
    security:
      enabled: false
```

This will:
- ✅ Skip X-Tenant-ID and X-API-Key validation
- ✅ Allow requests without authentication headers
- ⚠️ **Use only in development!**

## Stub TenantRegistry

Currently using `StubTenantRegistry` which:
- Accepts any tenant ID and API key
- Returns a hardcoded tenant
- Logs warnings about being a stub

**Location:** `app/src/main/kotlin/nl/compilot/ai/config/StubTenantRegistry.kt`

**TODO:** Replace with real implementation in Phase 3

## Database Setup Required

Before running the application, you must:

### 1. Create Master Database

```bash
psql -h localhost -U compilot -d postgres -c "CREATE DATABASE compilot_master"
```

### 2. Run Master Schema Setup

```bash
psql -h localhost -U compilot -d compilot_master -f setup-master-database.sql
```

This creates the `master` schema with tables:
- `master.tenants`
- `master.tenant_usage`
- `master.api_keys`
- `master.audit_log`

### 3. Verify Setup

```bash
psql -h localhost -U compilot -d compilot_master -c "\dt master.*"
```

Should show:
```
           List of relations
 Schema |     Name      | Type  |  Owner
--------+---------------+-------+----------
 master | api_keys      | table | compilot
 master | audit_log     | table | compilot
 master | tenant_usage  | table | compilot
 master | tenants       | table | compilot
```

## Starting the Application

```bash
cd core-ms/app
mvn spring-boot:run
```

Expected output:
```
Started CompilotAiApplication in X.XXX seconds
```

## Testing Without Tenant Database

The stub registry allows testing without creating tenant databases:

```bash
# No authentication (if security.enabled=false)
curl http://localhost:8080/health

# With stub authentication (if security.enabled=true)
curl -H "X-Tenant-ID: test" \
     -H "X-API-Key: any-key" \
     http://localhost:8080/health
```

The stub will log warnings:
```
WARN StubTenantRegistry - STUB: Using stub TenantRegistry - Replace with real implementation in Phase 3
```

## Known Limitations (Phase 2)

- ❌ No real tenant validation (stub only)
- ❌ No tenant database creation
- ❌ No API key hashing/validation
- ❌ TenantDataSourceRouter won't work without tenant databases
- ❌ No persistence (in-memory stub only)

These will be addressed in **Phase 3: Tenant Registry & Management**

## Troubleshooting

### Error: "Failed to configure a DataSource"

**Cause:** Missing database configuration or PostgreSQL not running

**Fix:**
1. Ensure PostgreSQL is running: `brew services start postgresql@16`
2. Verify database exists: `psql -l | grep compilot_master`
3. Check credentials in application.yml

### Error: "Failed to determine a suitable driver class"

**Cause:** PostgreSQL driver not on classpath

**Fix:** Ensure `app/pom.xml` has:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Warning: "STUB: Using stub TenantRegistry"

**Expected:** This is normal in Phase 2. The stub will be replaced in Phase 3.

### Error: "No tenant context set for current thread"

**Cause:** Trying to access tenant data without authentication

**Fix:** Either:
1. Disable security: `compilot.tenant.security.enabled=false`
2. Add headers: `X-Tenant-ID` and `X-API-Key`

---

**Next:** Phase 3 will implement real TenantRegistry with database persistence
