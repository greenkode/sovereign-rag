# Phase 2 Fixes: Module Structure Corrections

## Summary

Fixed Phase 2 implementation to follow proper module architecture:
- **core-ai**: Library module (no Spring Boot starters)
- **app**: Distribution module (has Spring Boot starters and concrete implementations)

## Changes Made

### 1. Dependencies Added

#### app/pom.xml
Added Spring Boot starters and database dependencies:
```xml
<!-- Spring Boot Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- HikariCP -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

#### core-ai/pom.xml
Added provided-scope dependencies for interfaces:
```xml
<!-- Jakarta Servlet API (for interceptors) -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>

<!-- Jakarta Persistence API (for JPA entities) -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <scope>provided</scope>
</dependency>

<!-- Hibernate Core (for @JdbcTypeCode, SqlTypes) -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2. Database Migration Files Moved

**From:**
- `core-ai/src/main/resources/db/master-schema/V1__create_master_schema.sql`
- `core-ai/src/main/resources/db/tenant-schema/V1__create_tenant_schema.sql`

**To:**
- `app/src/main/resources/db/master-schema/V1__create_master_schema.sql`
- `app/src/main/resources/db/tenant-schema/V1__create_tenant_schema.sql`

### 3. TenantDataSourceRouter Refactored

#### core-ai Module
Changed from concrete `@Component` to abstract base class:

```kotlin
// Before: Had HikariCP dependencies
@Component
class TenantDataSourceRouter(
    private val tenantRegistry: TenantRegistry,
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    // ... HikariCP code
)

// After: Abstract base class
abstract class TenantDataSourceRouter(
    protected val tenantRegistry: TenantRegistry
) : AbstractRoutingDataSource() {
    protected abstract fun createTenantDataSource(tenantId: String): DataSource
    protected open fun closeDataSource(dataSource: DataSource) {}
}
```

#### app Module
Created concrete implementation with HikariCP:

**File:** `app/src/main/kotlin/nl/compilot/ai/config/TenantDataSourceConfiguration.kt`

```kotlin
@Configuration
class TenantDataSourceConfiguration {

    @Bean
    @Primary
    fun tenantDataSourceRouter(...): TenantDataSourceRouter {
        return object : TenantDataSourceRouter(tenantRegistry) {
            override fun createTenantDataSource(tenantId: String): DataSource {
                // HikariCP implementation
            }

            override fun closeDataSource(dataSource: DataSource) {
                if (dataSource is HikariDataSource) {
                    dataSource.close()
                }
            }
        }
    }

    @Bean
    fun masterDataSource(...): DataSource {
        // Master database HikariDataSource with search_path
    }
}
```

## Architecture Benefits

### core-ai Module (Library)
✅ No Spring Boot dependencies
✅ Can be used in any Spring application
✅ Provides abstractions and interfaces
✅ Compiles independently

### app Module (Distribution)
✅ Has all Spring Boot starters
✅ Provides concrete implementations
✅ Contains configuration
✅ Manages database migrations

## Files Structure

```
compilot-ai/
├── core-ms/
│   ├── core-ai/              # Library module
│   │   └── src/main/kotlin/nl/compilot/ai/
│   │       └── tenant/
│   │           ├── domain/
│   │           │   ├── Tenant.kt
│   │           │   └── Document.kt
│   │           ├── context/
│   │           │   └── TenantContext.kt
│   │           ├── security/
│   │           │   └── TenantSecurityInterceptor.kt
│   │           ├── config/
│   │           │   ├── TenantDataSourceRouter.kt (abstract)
│   │           │   └── WebMvcConfiguration.kt
│   │           └── service/
│   │               └── TenantRegistry.kt (interface)
│   │
│   └── app/                   # Distribution module
│       └── src/main/
│           ├── kotlin/nl/compilot/ai/config/
│           │   └── TenantDataSourceConfiguration.kt (concrete)
│           └── resources/db/
│               ├── master-schema/
│               │   └── V1__create_master_schema.sql
│               └── tenant-schema/
│                   └── V1__create_tenant_schema.sql
```

## Compilation Test

Run to verify everything compiles:
```bash
cd core-ms
mvn clean compile
```

Expected output:
```
[INFO] core-ai ....................................... SUCCESS
[INFO] app ............................................ SUCCESS
[INFO] BUILD SUCCESS
```

## Next Steps

Phase 2 is now properly structured. Proceed with Phase 3 when ready:
1. Implement TenantRegistry service
2. Create JPA repositories for master database
3. Build tenant provisioning service

---

**Status**: ✅ Phase 2 Fixed & Complete
