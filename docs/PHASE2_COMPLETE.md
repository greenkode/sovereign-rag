# Phase 2: Core Infrastructure - COMPLETED

Phase 2 has been completed successfully. This phase established the foundational infrastructure for multi-tenant support.

## What Was Built

### 1. Domain Models

#### `Tenant.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/domain/Tenant.kt)
- Represents a WordPress site using Compilot AI
- Contains tenant metadata, limits, quotas, and settings
- Includes `TenantStatus` enum (ACTIVE, SUSPENDED, DELETED)

#### `Document.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/domain/Document.kt)
- JPA entity for document storage in tenant databases
- **pgvector support**: `vector(1024)` column for embeddings
- **JSONB support**: Flexible metadata storage
- Includes soft delete capability

### 2. Tenant Context Management

#### `TenantContext.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/context/TenantContext.kt)
- ThreadLocal storage for current tenant ID
- Allows any part of application to access current tenant
- **Critical**: Must be cleared after each request to prevent memory leaks

### 3. Security & Authentication

#### `TenantSecurityInterceptor.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/security/TenantSecurityInterceptor.kt)
- Validates tenant credentials on every API request
- Checks `X-Tenant-ID` and `X-API-Key` headers
- Sets tenant context for the request
- Cleans up context after request completion
- Skips health check and actuator endpoints

#### `WebMvcConfiguration.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/config/WebMvcConfiguration.kt)
- Registers the security interceptor
- Configures path patterns for authentication

### 4. Dynamic DataSource Routing

#### `TenantDataSourceRouter.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/config/TenantDataSourceRouter.kt)
- **Abstract base class** for routing database connections
- Routes based on TenantContext
- Connection caching for performance
- Library module (no Spring Boot dependencies)

#### `TenantDataSourceConfiguration.kt` (app/src/main/kotlin/nl/compilot/ai/config/TenantDataSourceConfiguration.kt)
- **Concrete implementation** with HikariCP
- Creates isolated connection pools per tenant
- **10 connections max** per tenant pool
- **Master pool**: 20 connections, sets search_path to master schema
- Lives in app module (has access to Spring Boot starters)

### 5. Service Interfaces

#### `TenantRegistry.kt` (core-ai/src/main/kotlin/nl/compilot/ai/tenant/service/TenantRegistry.kt)
- Interface for tenant validation and lookup
- Will be implemented in Phase 3

## Architecture Flow

```
1. HTTP Request arrives with headers:
   - X-Tenant-ID: tenant123
   - X-API-Key: abc...xyz

2. TenantSecurityInterceptor (preHandle):
   ├─ Validates headers are present
   ├─ Calls TenantRegistry.validateTenant()
   ├─ Checks tenant status is ACTIVE
   └─ Sets TenantContext.setCurrentTenant("tenant123")

3. Application code executes:
   └─ Any database query triggers TenantDataSourceRouter
       ├─ Reads TenantContext.getCurrentTenant()
       ├─ Gets tenant from registry
       ├─ Creates/reuses datasource for tenant database
       └─ Executes query on compilot_tenant_tenant123

4. TenantSecurityInterceptor (afterCompletion):
   └─ Calls TenantContext.clear() to clean up
```

## Database Strategy

### Master Database: `compilot_master`
- Schema: `master`
- Stores: tenants, api_keys, tenant_usage, audit_log
- Connection: Default datasource when no tenant context

### Tenant Databases: `compilot_tenant_<tenant_id>`
- Schema: `public`
- Stores: documents, chat_sessions, chat_messages, etc.
- Connection: Dynamic datasource based on TenantContext

## Configuration Requirements

Add to `application.yml`:

```yaml
spring:
  datasource:
    # Master database connection
    url: jdbc:postgresql://localhost:5432/compilot_master
    username: compilot
    password: RespectTheHangover

    # PostgreSQL host and port for tenant databases
    host: localhost
    port: 5432

    hikari:
      # Master database pool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-init-sql: SET search_path TO master, public
```

## Module Structure

### core-ai Module (Library)
Contains:
- Domain models (Tenant, Document)
- TenantContext (ThreadLocal)
- TenantSecurityInterceptor
- TenantDataSourceRouter (abstract)
- TenantRegistry interface

Dependencies:
- Jakarta Servlet API (provided scope)
- Jakarta Persistence API (provided scope)
- Hibernate Core (provided scope)

### app Module (Distribution)
Contains:
- TenantDataSourceConfiguration (concrete implementation)
- Database migration files (Flyway)
- application.yml configuration

Dependencies:
- spring-boot-starter-data-jpa
- spring-boot-starter-web
- postgresql (runtime)
- HikariCP
- flyway-core
- flyway-database-postgresql

## Dependencies Added

### app/pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### core-ai/pom.xml
```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <scope>provided</scope>
</dependency>
```

## What Phase 2 Does NOT Include

- ❌ TenantRegistry implementation (Phase 3)
- ❌ Master database repositories (Phase 3)
- ❌ Tenant provisioning service (Phase 3)
- ❌ Migration from Neo4j to PostgreSQL (Phase 4)
- ❌ pgvector repository implementation (Phase 4)

## Database Migration Files

Moved to app module:
- `app/src/main/resources/db/master-schema/V1__create_master_schema.sql`
- `app/src/main/resources/db/tenant-schema/V1__create_tenant_schema.sql`

Flyway configuration (add to application.yml):
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/master-schema
    baseline-on-migrate: true
```

## Testing Phase 2

You can test the infrastructure once Phase 3 is complete. For now, the code compiles but won't function without:

1. TenantRegistry implementation
2. Master database populated with tenants
3. Tenant databases created

## Compilation

To verify Phase 2 compiles correctly:
```bash
cd core-ms
mvn clean compile
```

## Security Notes

🔒 **Authentication Headers Required**:
- All API requests must include `X-Tenant-ID` and `X-API-Key`
- Interceptor validates on every request (except health checks)

🔒 **API Key Hashing**:
- API keys stored as SHA-256 hashes in database
- Never log or expose raw API keys

🔒 **Tenant Status**:
- Only ACTIVE tenants can make requests
- SUSPENDED/DELETED tenants receive 403 Forbidden

## Next Steps

Proceed to **Phase 3: Tenant Registry & Management** to:
1. Implement TenantRegistry service
2. Create JPA repositories for master database
3. Build tenant provisioning service
4. Create API endpoints for tenant management

---

**Status**: ✅ Phase 2 Complete
**Next**: Phase 3 - Tenant Registry & Management
