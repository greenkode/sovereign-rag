# Migration Plan: Neo4j → PostgreSQL Multi-Database with pgvector

## Executive Summary

**Goal**: Replace Neo4j with PostgreSQL + pgvector, implementing multi-tenant isolation with separate databases per tenant.

**Timeline**: 2-3 weeks (with testing)

**Risk Level**: Medium (requires data migration + architecture changes)

**Rollback Strategy**: Keep Neo4j running in parallel during Phase 3-4, switch back if issues arise

---

## Phase 0: Preparation & Setup (Days 1-2)

### 0.1 Environment Setup

**Tasks:**
- [ ] Install PostgreSQL 16+ on development environment
- [ ] Install pgvector extension
- [ ] Set up connection pooling (HikariCP configuration)
- [ ] Configure PostgreSQL for vector operations
- [ ] Set up database backup system

**PostgreSQL Configuration:**
```sql
-- postgresql.conf optimizations for pgvector
shared_preload_libraries = 'vector'
max_connections = 300  # For multi-tenant connection pooling
shared_buffers = 4GB
effective_cache_size = 12GB
maintenance_work_mem = 1GB
work_mem = 50MB

# For vector operations
max_parallel_workers_per_gather = 4
```

**Deliverables:**
- PostgreSQL 16 running with pgvector extension
- Database backup script ready
- Monitoring dashboard configured

---

## Phase 1: Schema Design & Master Database (Days 3-4)

### 1.1 Create Master Database Schema

**File: `/core-ms/core-ai/src/main/resources/db/master-schema/V1__create_master_schema.sql`**

```sql
-- Master database for tenant metadata
CREATE DATABASE sovereignrag_master;

\c sovereignrag_master;

-- Tenant registry
CREATE TABLE tenants (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    database_name VARCHAR(255) NOT NULL UNIQUE,
    api_key_hash VARCHAR(512) NOT NULL,
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted')),

    -- Limits and quotas
    max_documents INT DEFAULT 10000,
    max_embeddings INT DEFAULT 50000,
    max_requests_per_day INT DEFAULT 10000,

    -- Billing info (for future)
    subscription_tier VARCHAR(50) DEFAULT 'free',

    -- Contact info
    contact_email VARCHAR(500),
    contact_name VARCHAR(500),

    -- WordPress site info
    wordpress_url TEXT,
    wordpress_version VARCHAR(50),
    plugin_version VARCHAR(50),

    -- Features and settings
    features JSONB DEFAULT '{}'::jsonb,
    settings JSONB DEFAULT '{}'::jsonb,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_active_at TIMESTAMP,

    -- Soft delete
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_tenants_api_key ON tenants(api_key_hash);
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_database_name ON tenants(database_name);

-- Tenant usage tracking
CREATE TABLE tenant_usage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) REFERENCES tenants(id) ON DELETE CASCADE,
    date DATE NOT NULL,

    -- Counters
    api_requests INT DEFAULT 0,
    documents_ingested INT DEFAULT 0,
    search_queries INT DEFAULT 0,
    chat_messages INT DEFAULT 0,

    -- Resources
    storage_bytes BIGINT DEFAULT 0,

    -- Costs (for billing)
    embedding_tokens BIGINT DEFAULT 0,
    llm_tokens BIGINT DEFAULT 0,

    created_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(tenant_id, date)
);

CREATE INDEX idx_tenant_usage_tenant_date ON tenant_usage(tenant_id, date);

-- API key management (support multiple keys per tenant)
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) REFERENCES tenants(id) ON DELETE CASCADE,
    key_hash VARCHAR(512) NOT NULL UNIQUE,
    name VARCHAR(255),

    -- Permissions (for future fine-grained access)
    permissions JSONB DEFAULT '["read", "write"]'::jsonb,

    -- Key metadata
    created_at TIMESTAMP DEFAULT NOW(),
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,

    -- Track usage per key
    usage_count BIGINT DEFAULT 0
);

CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);

-- Audit log
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    user_agent TEXT,
    ip_address INET,
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id, created_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 1.2 Create Tenant Database Template Schema

**File: `/core-ms/core-ai/src/main/resources/db/tenant-schema/V1__create_tenant_schema.sql`**

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- For fuzzy text matching
CREATE EXTENSION IF NOT EXISTS btree_gin; -- For JSONB indexing

-- Documents table (main content storage)
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Content
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    url TEXT,
    source TEXT,

    -- Document metadata
    document_type VARCHAR(50) DEFAULT 'page',  -- page, post, product, etc.
    language VARCHAR(10) DEFAULT 'en',
    author VARCHAR(255),

    -- Vector embedding (1024 dimensions for mxbai-embed-large)
    embedding vector(1024),

    -- Extensible metadata (WordPress custom fields, etc.)
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    indexed_at TIMESTAMP DEFAULT NOW(),

    -- Soft delete
    deleted_at TIMESTAMP
);

-- Indexes for documents
CREATE INDEX idx_documents_url ON documents(url) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_created ON documents(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_type ON documents(document_type) WHERE deleted_at IS NULL;

-- Vector similarity index (IVF + cosine distance)
CREATE INDEX idx_documents_embedding_ivfflat ON documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Full-text search index
CREATE INDEX idx_documents_content_fts ON documents
    USING gin(to_tsvector('english', content))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_documents_title_fts ON documents
    USING gin(to_tsvector('english', title))
    WHERE deleted_at IS NULL;

-- JSONB metadata index
CREATE INDEX idx_documents_metadata ON documents USING gin(metadata);

-- Trigram index for fuzzy matching
CREATE INDEX idx_documents_content_trgm ON documents USING gin(content gin_trgm_ops);

-- Document segments (for chunked content)
CREATE TABLE document_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,

    -- Segment data
    segment_index INT NOT NULL,
    content TEXT NOT NULL,
    token_count INT,

    -- Vector embedding
    embedding vector(1024),

    -- Metadata
    metadata JSONB DEFAULT '{}'::jsonb,

    created_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(document_id, segment_index)
);

CREATE INDEX idx_segments_document ON document_segments(document_id);
CREATE INDEX idx_segments_embedding_ivfflat ON document_segments
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Chat sessions
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Session metadata
    persona VARCHAR(100) DEFAULT 'customer_service',
    language VARCHAR(10),

    -- State
    status VARCHAR(50) DEFAULT 'active',

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),
    closed_at TIMESTAMP,

    -- Session context
    context JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_chat_sessions_created ON chat_sessions(created_at DESC);
CREATE INDEX idx_chat_sessions_status ON chat_sessions(status);
CREATE INDEX idx_chat_sessions_last_activity ON chat_sessions(last_activity_at DESC);

-- Chat messages
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE CASCADE,

    -- Message data
    role VARCHAR(50) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    message TEXT NOT NULL,

    -- AI response metadata
    confidence_score INT,
    show_confidence BOOLEAN DEFAULT true,
    sources JSONB,

    -- Flags
    suggests_escalation BOOLEAN DEFAULT false,
    suggests_close BOOLEAN DEFAULT false,
    escalation_requested BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),

    -- Message context
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at);
CREATE INDEX idx_chat_messages_role ON chat_messages(role);

-- Escalations (customer support handoff)
CREATE TABLE escalations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,

    -- Escalation details
    reason TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    priority VARCHAR(20) DEFAULT 'normal',

    -- User contact info
    user_email VARCHAR(500),
    user_name VARCHAR(500),
    user_phone VARCHAR(100),

    -- Assignment
    assigned_to VARCHAR(255),
    assigned_at TIMESTAMP,

    -- Resolution
    resolved_at TIMESTAMP,
    resolution_notes TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Additional data
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_escalations_session ON escalations(session_id);
CREATE INDEX idx_escalations_status ON escalations(status);
CREATE INDEX idx_escalations_created ON escalations(created_at DESC);

-- Unanswered queries (for content gap analysis)
CREATE TABLE unanswered_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Query details
    query TEXT NOT NULL,
    language VARCHAR(10),

    -- Context
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,

    -- Analysis
    confidence_score DOUBLE PRECISION,
    reason VARCHAR(255),

    -- Resolution tracking
    status VARCHAR(50) DEFAULT 'open',
    resolved_at TIMESTAMP,
    resolution_notes TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),

    -- Count occurrences
    occurrence_count INT DEFAULT 1,
    last_occurred_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_unanswered_queries_query ON unanswered_queries(query);
CREATE INDEX idx_unanswered_queries_status ON unanswered_queries(status);
CREATE INDEX idx_unanswered_queries_created ON unanswered_queries(created_at DESC);

-- Feedback (user ratings)
CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,
    message_id UUID REFERENCES chat_messages(id) ON DELETE SET NULL,

    -- Feedback data
    query TEXT NOT NULL,
    is_accurate BOOLEAN,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    feedback_text TEXT,

    -- User info (optional)
    user_email VARCHAR(500),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_feedback_session ON feedback(session_id);
CREATE INDEX idx_feedback_is_accurate ON feedback(is_accurate);
CREATE INDEX idx_feedback_created ON feedback(created_at DESC);

-- Update triggers
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_sessions_updated_at BEFORE UPDATE ON chat_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_escalations_updated_at BEFORE UPDATE ON escalations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Helper function for vector similarity search with filters
CREATE OR REPLACE FUNCTION search_documents(
    query_embedding vector(1024),
    match_threshold double precision DEFAULT 0.5,
    match_count int DEFAULT 10,
    filter_type varchar DEFAULT NULL,
    filter_date_after timestamp DEFAULT NULL
)
RETURNS TABLE (
    id uuid,
    title text,
    content text,
    url text,
    similarity double precision
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.id,
        d.title,
        d.content,
        d.url,
        1 - (d.embedding <=> query_embedding) as similarity
    FROM documents d
    WHERE
        d.deleted_at IS NULL
        AND (filter_type IS NULL OR d.document_type = filter_type)
        AND (filter_date_after IS NULL OR d.created_at > filter_date_after)
        AND 1 - (d.embedding <=> query_embedding) >= match_threshold
    ORDER BY d.embedding <=> query_embedding
    LIMIT match_count;
END;
$$ LANGUAGE plpgsql;
```

**Deliverables:**
- Master database schema (tenants registry)
- Tenant database template schema
- SQL migration scripts in Flyway format

---

## Phase 2: Core Infrastructure (Days 5-7)

### 2.1 Kotlin Domain Models

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/tenant/domain/Tenant.kt`**

```kotlin
package ai.sovereignrag.tenant.domain

import java.time.LocalDateTime

data class Tenant(
    val id: String,
    val name: String,
    val databaseName: String,
    val apiKeyHash: String,
    val status: TenantStatus = TenantStatus.ACTIVE,
    val maxDocuments: Int = 10000,
    val maxRequestsPerDay: Int = 10000,
    val contactEmail: String? = null,
    val wordpressUrl: String? = null,
    val features: Map<String, Any> = emptyMap(),
    val settings: Map<String, Any> = emptyMap(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastActiveAt: LocalDateTime? = null
)

enum class TenantStatus {
    ACTIVE, SUSPENDED, DELETED
}
```

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/content/domain/Document.kt`**

```kotlin
package ai.sovereignrag.content.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    val url: String? = null,
    val source: String? = null,

    @Column(name = "document_type")
    val documentType: String = "page",

    val language: String = "en",
    val author: String? = null,

    // pgvector type - will be mapped with custom type
    @Column(columnDefinition = "vector(1024)")
    val embedding: FloatArray? = null,

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter::class)
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "indexed_at")
    val indexedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Document) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
```

### 2.2 Tenant Context & Security

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/tenant/context/TenantContext.kt`**

```kotlin
package ai.sovereignrag.tenant.context

/**
 * Thread-local storage for current tenant context
 */
object TenantContext {
    private val currentTenant = ThreadLocal<String>()

    fun setCurrentTenant(tenantId: String) {
        currentTenant.set(tenantId)
    }

    fun getCurrentTenant(): String {
        return currentTenant.get()
            ?: throw TenantContextException("No tenant context set for current thread")
    }

    fun getCurrentTenantOrNull(): String? {
        return currentTenant.get()
    }

    fun clear() {
        currentTenant.remove()
    }
}

class TenantContextException(message: String) : RuntimeException(message)
```

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/tenant/security/TenantSecurityInterceptor.kt`**

```kotlin
package ai.sovereignrag.tenant.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import ai.sovereignrag.tenant.context.TenantContext
import ai.sovereignrag.tenant.service.TenantRegistry
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val logger = KotlinLogging.logger {}

@Component
class TenantSecurityInterceptor(
    private val tenantRegistry: TenantRegistry
) : HandlerInterceptor {

    companion object {
        const val HEADER_TENANT_ID = "X-Tenant-ID"
        const val HEADER_API_KEY = "X-API-Key"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val tenantId = request.getHeader(HEADER_TENANT_ID)
        val apiKey = request.getHeader(HEADER_API_KEY)

        if (tenantId.isNullOrBlank()) {
            logger.warn { "Missing tenant ID in request: ${request.requestURI}" }
            response.sendError(401, "Missing X-Tenant-ID header")
            return false
        }

        if (apiKey.isNullOrBlank()) {
            logger.warn { "Missing API key for tenant $tenantId" }
            response.sendError(401, "Missing X-API-Key header")
            return false
        }

        // Validate tenant and API key
        val tenant = try {
            tenantRegistry.validateTenant(tenantId, apiKey)
        } catch (e: Exception) {
            logger.error(e) { "Error validating tenant $tenantId" }
            response.sendError(500, "Internal server error")
            return false
        }

        if (tenant == null) {
            logger.warn { "Invalid credentials for tenant $tenantId" }
            response.sendError(403, "Invalid tenant credentials")
            return false
        }

        if (tenant.status != TenantStatus.ACTIVE) {
            logger.warn { "Tenant $tenantId is not active: ${tenant.status}" }
            response.sendError(403, "Tenant account is ${tenant.status.name.lowercase()}")
            return false
        }

        // Set tenant context for this request
        TenantContext.setCurrentTenant(tenantId)
        logger.debug { "Tenant context set: $tenantId" }

        // Update last active timestamp
        tenantRegistry.updateLastActive(tenantId)

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Always clean up tenant context
        TenantContext.clear()
    }
}
```

### 2.3 Dynamic DataSource Routing

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/config/TenantDataSourceRouter.kt`**

```kotlin
package ai.sovereignrag.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import ai.sovereignrag.tenant.context.TenantContext
import ai.sovereignrag.tenant.service.TenantRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Component
class TenantDataSourceRouter(
    private val tenantRegistry: TenantRegistry,
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int
) : AbstractRoutingDataSource() {

    private val dataSourceCache = ConcurrentHashMap<String, DataSource>()

    override fun determineCurrentLookupKey(): String {
        return TenantContext.getCurrentTenantOrNull() ?: "master"
    }

    override fun determineTargetDataSource(): DataSource {
        val tenantId = TenantContext.getCurrentTenantOrNull()

        // If no tenant context, use master database
        if (tenantId == null) {
            return resolvedDefaultDataSource
                ?: throw IllegalStateException("Default datasource not configured")
        }

        // Get or create datasource for this tenant
        return dataSourceCache.computeIfAbsent(tenantId) { tid ->
            createTenantDataSource(tid)
        }
    }

    private fun createTenantDataSource(tenantId: String): DataSource {
        val tenant = tenantRegistry.getTenant(tenantId)

        logger.info { "Creating datasource for tenant: $tenantId (database: ${tenant.databaseName})" }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/${tenant.databaseName}"
            username = dbUsername
            password = dbPassword

            // Connection pool settings (per tenant)
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes

            // Pool name for monitoring
            poolName = "tenant-${tenant.id}"

            // Performance settings
            cachePrepStmts = true
            prepStmtCacheSize = 250
            prepStmtCacheSqlLimit = 2048

            // Validation
            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
        }

        return HikariDataSource(config)
    }

    /**
     * Close datasource for a specific tenant (e.g., when tenant is deleted)
     */
    fun closeTenantDataSource(tenantId: String) {
        dataSourceCache.remove(tenantId)?.let { ds ->
            if (ds is HikariDataSource) {
                logger.info { "Closing datasource for tenant: $tenantId" }
                ds.close()
            }
        }
    }

    /**
     * Get current pool statistics for monitoring
     */
    fun getPoolStats(): Map<String, Map<String, Any>> {
        return dataSourceCache.mapValues { (_, ds) ->
            if (ds is HikariDataSource) {
                mapOf(
                    "active" to ds.hikariPoolMXBean.activeConnections,
                    "idle" to ds.hikariPoolMXBean.idleConnections,
                    "total" to ds.hikariPoolMXBean.totalConnections,
                    "waiting" to ds.hikariPoolMXBean.threadsAwaitingConnection
                )
            } else {
                emptyMap()
            }
        }
    }
}
```

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/config/DataSourceConfig.kt`**

```kotlin
package ai.sovereignrag.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

@Configuration
class DataSourceConfig(
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int
) {

    /**
     * Master database - for tenant registry and system-wide data
     */
    @Bean(name = ["masterDataSource"])
    fun masterDataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/sovereignrag_master"
            username = dbUsername
            password = dbPassword
            maximumPoolSize = 20
            minimumIdle = 5
            poolName = "master-pool"
            connectionTimeout = 30000
        }
        return HikariDataSource(config)
    }

    /**
     * Routing datasource - routes to tenant-specific databases
     */
    @Bean
    @Primary
    fun dataSource(
        tenantDataSourceRouter: TenantDataSourceRouter,
        @Qualifier("masterDataSource") masterDataSource: DataSource
    ): DataSource {
        // Set master as default datasource
        tenantDataSourceRouter.setDefaultTargetDataSource(masterDataSource)
        tenantDataSourceRouter.afterPropertiesSet()

        // Wrap in lazy proxy to avoid connection on every request
        return LazyConnectionDataSourceProxy(tenantDataSourceRouter)
    }
}
```

**Deliverables:**
- Tenant context management (ThreadLocal)
- Security interceptor for API authentication
- Dynamic datasource routing
- Connection pool per tenant

---

## Phase 3: Tenant Registry & Management (Days 8-9)

### 3.1 Tenant Registry Service

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/tenant/service/TenantRegistry.kt`**

```kotlin
package ai.sovereignrag.tenant.service

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import ai.sovereignrag.tenant.domain.Tenant
import ai.sovereignrag.tenant.domain.TenantStatus
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.sql.DriverManager
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Service
class TenantRegistry(
    @Qualifier("masterDataSource") masterDataSource: DataSource,
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int
) {

    private val masterJdbcTemplate = JdbcTemplate(masterDataSource)
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val secureRandom = SecureRandom()

    /**
     * Get tenant by ID (cached)
     */
    @Cacheable(cacheNames = ["tenants"], key = "#tenantId")
    fun getTenant(tenantId: String): Tenant {
        val sql = """
            SELECT id, name, database_name, api_key_hash, status,
                   max_documents, max_requests_per_day,
                   contact_email, wordpress_url,
                   features, settings,
                   created_at, updated_at, last_active_at
            FROM tenants
            WHERE id = ? AND deleted_at IS NULL
        """

        return masterJdbcTemplate.queryForObject(sql, tenantId) { rs, _ ->
            Tenant(
                id = rs.getString("id"),
                name = rs.getString("name"),
                databaseName = rs.getString("database_name"),
                apiKeyHash = rs.getString("api_key_hash"),
                status = TenantStatus.valueOf(rs.getString("status").uppercase()),
                maxDocuments = rs.getInt("max_documents"),
                maxRequestsPerDay = rs.getInt("max_requests_per_day"),
                contactEmail = rs.getString("contact_email"),
                wordpressUrl = rs.getString("wordpress_url"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
                lastActiveAt = rs.getTimestamp("last_active_at")?.toLocalDateTime()
            )
        } ?: throw TenantNotFoundException("Tenant not found: $tenantId")
    }

    /**
     * Validate tenant credentials
     */
    fun validateTenant(tenantId: String, apiKey: String): Tenant? {
        return try {
            val tenant = getTenant(tenantId)
            if (passwordEncoder.matches(apiKey, tenant.apiKeyHash)) {
                tenant
            } else {
                logger.warn { "Invalid API key for tenant: $tenantId" }
                null
            }
        } catch (e: TenantNotFoundException) {
            logger.warn { "Tenant not found: $tenantId" }
            null
        }
    }

    /**
     * Create new tenant with isolated database
     */
    @CacheEvict(cacheNames = ["tenants"], allEntries = true)
    fun createTenant(
        tenantId: String,
        name: String,
        contactEmail: String? = null,
        wordpressUrl: String? = null
    ): CreateTenantResult {
        logger.info { "Creating new tenant: $tenantId ($name)" }

        val databaseName = "sovereignrag_tenant_${tenantId.lowercase().replace("-", "_")}"
        val apiKey = generateApiKey()
        val apiKeyHash = passwordEncoder.encode(apiKey)

        try {
            // 1. Create PostgreSQL database
            createPostgreSQLDatabase(databaseName)

            // 2. Apply schema migrations to new database
            applySchemaToDatabase(databaseName)

            // 3. Register tenant in master database
            val sql = """
                INSERT INTO tenants (id, name, database_name, api_key_hash,
                                   contact_email, wordpress_url, status)
                VALUES (?, ?, ?, ?, ?, ?, 'active')
            """
            masterJdbcTemplate.update(
                sql, tenantId, name, databaseName, apiKeyHash,
                contactEmail, wordpressUrl
            )

            logger.info { "Tenant created successfully: $tenantId" }

            return CreateTenantResult(
                tenant = getTenant(tenantId),
                apiKey = apiKey
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to create tenant: $tenantId" }
            // Cleanup on failure
            try {
                dropDatabase(databaseName)
            } catch (cleanupEx: Exception) {
                logger.error(cleanupEx) { "Failed to cleanup database: $databaseName" }
            }
            throw TenantCreationException("Failed to create tenant: ${e.message}", e)
        }
    }

    /**
     * Update tenant's last active timestamp
     */
    fun updateLastActive(tenantId: String) {
        val sql = "UPDATE tenants SET last_active_at = NOW() WHERE id = ?"
        masterJdbcTemplate.update(sql, tenantId)
    }

    /**
     * List all active tenants
     */
    fun listTenants(status: TenantStatus? = null): List<Tenant> {
        val sql = if (status != null) {
            """
            SELECT id, name, database_name, api_key_hash, status,
                   max_documents, max_requests_per_day,
                   contact_email, wordpress_url,
                   features, settings,
                   created_at, updated_at, last_active_at
            FROM tenants
            WHERE status = ? AND deleted_at IS NULL
            ORDER BY created_at DESC
            """
        } else {
            """
            SELECT id, name, database_name, api_key_hash, status,
                   max_documents, max_requests_per_day,
                   contact_email, wordpress_url,
                   features, settings,
                   created_at, updated_at, last_active_at
            FROM tenants
            WHERE deleted_at IS NULL
            ORDER BY created_at DESC
            """
        }

        return if (status != null) {
            masterJdbcTemplate.query(sql, status.name.lowercase()) { rs, _ -> mapTenant(rs) }
        } else {
            masterJdbcTemplate.query(sql) { rs, _ -> mapTenant(rs) }
        }
    }

    /**
     * Delete tenant and its database (soft delete)
     */
    @CacheEvict(cacheNames = ["tenants"], key = "#tenantId")
    fun deleteTenant(tenantId: String, hardDelete: Boolean = false) {
        val tenant = getTenant(tenantId)

        if (hardDelete) {
            logger.warn { "HARD DELETE tenant: $tenantId" }
            // Drop the database
            dropDatabase(tenant.databaseName)

            // Delete from registry
            masterJdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId)
        } else {
            logger.info { "Soft delete tenant: $tenantId" }
            // Soft delete (mark as deleted)
            val sql = """
                UPDATE tenants
                SET status = 'deleted', deleted_at = NOW(), updated_at = NOW()
                WHERE id = ?
            """
            masterJdbcTemplate.update(sql, tenantId)
        }
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private fun createPostgreSQLDatabase(databaseName: String) {
        logger.info { "Creating PostgreSQL database: $databaseName" }

        val serverUrl = "jdbc:postgresql://$dbHost:$dbPort/postgres"
        DriverManager.getConnection(serverUrl, dbUsername, dbPassword).use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE DATABASE $databaseName
                    WITH OWNER = $dbUsername
                         ENCODING = 'UTF8'
                         LC_COLLATE = 'en_US.UTF-8'
                         LC_CTYPE = 'en_US.UTF-8'
                         TEMPLATE = template0
                """)
            }
        }

        // Enable extensions
        val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
        DriverManager.getConnection(dbUrl, dbUsername, dbPassword).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS btree_gin")
            }
        }

        logger.info { "Database created with extensions: $databaseName" }
    }

    private fun applySchemaToDatabase(databaseName: String) {
        logger.info { "Applying schema migrations to: $databaseName" }

        val flyway = Flyway.configure()
            .dataSource(
                "jdbc:postgresql://$dbHost:$dbPort/$databaseName",
                dbUsername,
                dbPassword
            )
            .locations("classpath:db/tenant-schema")
            .baselineOnMigrate(true)
            .load()

        val result = flyway.migrate()
        logger.info { "Applied ${result.migrationsExecuted} migrations to $databaseName" }
    }

    private fun dropDatabase(databaseName: String) {
        logger.warn { "Dropping database: $databaseName" }

        val serverUrl = "jdbc:postgresql://$dbHost:$dbPort/postgres"
        DriverManager.getConnection(serverUrl, dbUsername, dbPassword).use { conn ->
            conn.autoCommit = true

            // Terminate all connections to the database
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    SELECT pg_terminate_backend(pg_stat_activity.pid)
                    FROM pg_stat_activity
                    WHERE pg_stat_activity.datname = '$databaseName'
                      AND pid <> pg_backend_pid()
                """)
            }

            // Drop the database
            conn.createStatement().use { stmt ->
                stmt.execute("DROP DATABASE IF EXISTS $databaseName")
            }
        }
    }

    private fun generateApiKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun mapTenant(rs: java.sql.ResultSet): Tenant {
        return Tenant(
            id = rs.getString("id"),
            name = rs.getString("name"),
            databaseName = rs.getString("database_name"),
            apiKeyHash = rs.getString("api_key_hash"),
            status = TenantStatus.valueOf(rs.getString("status").uppercase()),
            maxDocuments = rs.getInt("max_documents"),
            maxRequestsPerDay = rs.getInt("max_requests_per_day"),
            contactEmail = rs.getString("contact_email"),
            wordpressUrl = rs.getString("wordpress_url"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
            lastActiveAt = rs.getTimestamp("last_active_at")?.toLocalDateTime()
        )
    }
}

data class CreateTenantResult(
    val tenant: Tenant,
    val apiKey: String  // Only returned once!
)

class TenantNotFoundException(message: String) : RuntimeException(message)
class TenantCreationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

### 3.2 Tenant Management API

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/tenant/api/TenantController.kt`**

```kotlin
package ai.sovereignrag.tenant.api

import ai.sovereignrag.tenant.service.TenantRegistry
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tenants")
class TenantController(
    private val tenantRegistry: TenantRegistry
) {

    /**
     * Create new tenant
     * Admin-only endpoint (add @PreAuthorize later)
     */
    @PostMapping
    fun createTenant(@RequestBody request: CreateTenantRequest): CreateTenantResponse {
        val result = tenantRegistry.createTenant(
            tenantId = request.tenantId,
            name = request.name,
            contactEmail = request.contactEmail,
            wordpressUrl = request.wordpressUrl
        )

        return CreateTenantResponse(
            tenantId = result.tenant.id,
            apiKey = result.apiKey,
            databaseName = result.tenant.databaseName,
            message = "Tenant created successfully. Save the API key - it won't be shown again!"
        )
    }

    /**
     * List all tenants
     */
    @GetMapping
    fun listTenants(@RequestParam(required = false) status: String?): List<TenantSummary> {
        val tenantStatus = status?.let { TenantStatus.valueOf(it.uppercase()) }
        return tenantRegistry.listTenants(tenantStatus).map { tenant ->
            TenantSummary(
                id = tenant.id,
                name = tenant.name,
                status = tenant.status.name.lowercase(),
                wordpressUrl = tenant.wordpressUrl,
                createdAt = tenant.createdAt,
                lastActiveAt = tenant.lastActiveAt
            )
        }
    }

    /**
     * Get tenant details
     */
    @GetMapping("/{tenantId}")
    fun getTenant(@PathVariable tenantId: String): TenantDetails {
        val tenant = tenantRegistry.getTenant(tenantId)
        return TenantDetails(
            id = tenant.id,
            name = tenant.name,
            databaseName = tenant.databaseName,
            status = tenant.status.name.lowercase(),
            maxDocuments = tenant.maxDocuments,
            maxRequestsPerDay = tenant.maxRequestsPerDay,
            contactEmail = tenant.contactEmail,
            wordpressUrl = tenant.wordpressUrl,
            createdAt = tenant.createdAt,
            lastActiveAt = tenant.lastActiveAt
        )
    }

    /**
     * Delete tenant
     */
    @DeleteMapping("/{tenantId}")
    fun deleteTenant(
        @PathVariable tenantId: String,
        @RequestParam(defaultValue = "false") hardDelete: Boolean
    ): ResponseEntity<String> {
        tenantRegistry.deleteTenant(tenantId, hardDelete)
        return ResponseEntity.ok(
            if (hardDelete) "Tenant permanently deleted"
            else "Tenant marked as deleted"
        )
    }
}

// DTOs
data class CreateTenantRequest(
    val tenantId: String,
    val name: String,
    val contactEmail: String? = null,
    val wordpressUrl: String? = null
)

data class CreateTenantResponse(
    val tenantId: String,
    val apiKey: String,
    val databaseName: String,
    val message: String
)

data class TenantSummary(
    val id: String,
    val name: String,
    val status: String,
    val wordpressUrl: String?,
    val createdAt: LocalDateTime,
    val lastActiveAt: LocalDateTime?
)

data class TenantDetails(
    val id: String,
    val name: String,
    val databaseName: String,
    val status: String,
    val maxDocuments: Int,
    val maxRequestsPerDay: Int,
    val contactEmail: String?,
    val wordpressUrl: String?,
    val createdAt: LocalDateTime,
    val lastActiveAt: LocalDateTime?
)
```

**Deliverables:**
- Tenant registry service
- Tenant creation/deletion logic
- Database provisioning automation
- Admin API for tenant management

---

## Phase 4: Migrate Content Service to pgvector (Days 10-12)

### 4.1 Spring Data JPA Repositories

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/content/repository/DocumentRepository.kt`**

```kotlin
package ai.sovereignrag.content.repository

import ai.sovereignrag.content.domain.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface DocumentRepository : JpaRepository<Document, UUID> {

    /**
     * Find document by URL
     */
    fun findByUrlAndDeletedAtIsNull(url: String): Document?

    /**
     * Delete by URL (soft delete)
     */
    @Query("UPDATE Document d SET d.deletedAt = :now WHERE d.url = :url")
    fun softDeleteByUrl(@Param("url") url: String, @Param("now") now: LocalDateTime = LocalDateTime.now())

    /**
     * Vector similarity search
     */
    @Query(
        nativeQuery = true,
        value = """
            SELECT * FROM documents
            WHERE deleted_at IS NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
        """
    )
    fun findSimilar(
        @Param("embedding") embedding: FloatArray,
        @Param("limit") limit: Int
    ): List<Document>

    /**
     * Vector similarity search with confidence threshold
     */
    @Query(
        nativeQuery = true,
        value = """
            SELECT *, 1 - (embedding <=> CAST(:embedding AS vector)) as similarity
            FROM documents
            WHERE deleted_at IS NULL
              AND 1 - (embedding <=> CAST(:embedding AS vector)) >= :minConfidence
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
        """
    )
    fun findSimilarWithConfidence(
        @Param("embedding") embedding: FloatArray,
        @Param("minConfidence") minConfidence: Double,
        @Param("limit") limit: Int
    ): List<DocumentWithSimilarity>

    /**
     * Hybrid search: vector + full-text
     */
    @Query(
        nativeQuery = true,
        value = """
            WITH vector_results AS (
                SELECT
                    id,
                    1 - (embedding <=> CAST(:embedding AS vector)) as vector_score
                FROM documents
                WHERE deleted_at IS NULL
                ORDER BY embedding <=> CAST(:embedding AS vector)
                LIMIT 100
            ),
            text_results AS (
                SELECT
                    id,
                    ts_rank(to_tsvector('english', content), plainto_tsquery('english', :query)) as text_score
                FROM documents
                WHERE deleted_at IS NULL
                  AND to_tsvector('english', content) @@ plainto_tsquery('english', :query)
            )
            SELECT d.*,
                   COALESCE(v.vector_score, 0) * 0.7 + COALESCE(t.text_score, 0) * 0.3 as combined_score
            FROM documents d
            LEFT JOIN vector_results v ON d.id = v.id
            LEFT JOIN text_results t ON d.id = t.id
            WHERE (v.id IS NOT NULL OR t.id IS NOT NULL)
              AND d.deleted_at IS NULL
            ORDER BY combined_score DESC
            LIMIT :limit
        """
    )
    fun hybridSearch(
        @Param("query") query: String,
        @Param("embedding") embedding: FloatArray,
        @Param("limit") limit: Int
    ): List<Document>

    /**
     * Count documents
     */
    fun countByDeletedAtIsNull(): Long

    /**
     * Get all documents (for export/backup)
     */
    fun findAllByDeletedAtIsNull(): List<Document>
}

// Projection for similarity score
interface DocumentWithSimilarity {
    fun getId(): UUID
    fun getTitle(): String
    fun getContent(): String
    fun getUrl(): String?
    fun getSimilarity(): Double
}
```

### 4.2 New ContentService with pgvector

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/content/service/ContentServicePgVector.kt`**

```kotlin
package ai.sovereignrag.content.service

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.model.embedding.EmbeddingModel
import mu.KotlinLogging
import ai.sovereignrag.content.domain.Document
import ai.sovereignrag.content.repository.DocumentRepository
import ai.sovereignrag.domain.ContentDocument
import ai.sovereignrag.domain.SearchResult
import ai.sovereignrag.spell.service.SpellCorrectionService
import ai.sovereignrag.tenant.context.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class ContentServicePgVector(
    private val documentRepository: DocumentRepository,
    private val embeddingModel: EmbeddingModel,
    private val spellCorrectionService: SpellCorrectionService
) {

    private val documentSplitter: DocumentSplitter = DocumentSplitters.recursive(
        1000,  // Max chunk size
        200    // Overlap
    )

    /**
     * Ingest content document into tenant's database
     */
    fun ingest(contentDoc: ContentDocument) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Ingesting document: ${contentDoc.title} (${contentDoc.url})" }

        try {
            // Check if document already exists
            val existingDoc = contentDoc.url?.let {
                documentRepository.findByUrlAndDeletedAtIsNull(it)
            }

            if (existingDoc != null) {
                logger.info { "[$tenantId] Document exists, updating: ${contentDoc.url}" }
                // Delete old version (soft delete)
                documentRepository.softDeleteByUrl(contentDoc.url!!)
            }

            // Generate embedding for full content
            val embedding = embeddingModel.embed(contentDoc.content).content().vector()

            // Create and save document
            val document = Document(
                id = UUID.randomUUID(),
                title = contentDoc.title,
                content = contentDoc.content,
                url = contentDoc.url,
                source = contentDoc.source,
                embedding = embedding,
                metadata = contentDoc.metadata,
                createdAt = contentDoc.createdAt ?: LocalDateTime.now()
            )

            documentRepository.save(document)

            logger.info { "[$tenantId] Document ingested successfully: ${document.id}" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to ingest document: ${contentDoc.title}" }
            throw ContentIngestionException("Failed to ingest document: ${e.message}", e)
        }
    }

    /**
     * Semantic search using pgvector
     */
    fun search(
        query: String,
        numResults: Int = 5,
        language: String? = null
    ): List<SearchResult> {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Searching: $query (limit: $numResults)" }

        // Apply spell correction for English queries
        val correctedQuery = if (language == null || language.startsWith("en")) {
            spellCorrectionService.correctSpelling(query)
        } else {
            query
        }

        if (correctedQuery != query) {
            logger.info { "[$tenantId] Query corrected: '$query' → '$correctedQuery'" }
        }

        // Generate query embedding
        val queryEmbedding = embeddingModel.embed(correctedQuery).content().vector()

        // Perform hybrid search (vector + full-text)
        val documents = documentRepository.hybridSearch(
            query = correctedQuery,
            embedding = queryEmbedding,
            limit = numResults * 2  // Get more candidates for filtering
        )

        // Convert to SearchResult
        return documents.take(numResults).map { doc ->
            SearchResult(
                uuid = doc.id.toString(),
                fact = doc.content,
                confidence = calculateConfidence(doc.embedding!!, queryEmbedding),
                source = doc.url,
                validAt = doc.createdAt,
                metadata = doc.metadata
            )
        }
    }

    /**
     * Delete document by URL
     */
    fun deleteByUrl(url: String) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Deleting document: $url" }
        documentRepository.softDeleteByUrl(url)
    }

    /**
     * Get statistics for current tenant
     */
    fun getStats(): Map<String, Any> {
        val tenantId = TenantContext.getCurrentTenant()
        val docCount = documentRepository.countByDeletedAtIsNull()

        return mapOf(
            "tenant_id" to tenantId,
            "document_count" to docCount,
            "embedding_dimensions" to 1024,
            "model" to "mxbai-embed-large"
        )
    }

    /**
     * Export all documents for backup/migration
     */
    fun exportAllDocuments(): List<Document> {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Exporting all documents" }
        return documentRepository.findAllByDeletedAtIsNull()
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private fun calculateConfidence(embedding1: FloatArray, embedding2: FloatArray): Double {
        // Cosine similarity
        return 1.0 - cosineSimilarity(embedding1, embedding2)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "Vectors must have same dimensions" }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
    }
}

class ContentIngestionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

**Deliverables:**
- JPA entities for documents
- Repository with vector search queries
- New ContentService using pgvector
- Hybrid search (vector + full-text)

---

## Phase 5: Update Chat Services (Day 13)

### 5.1 Chat Session with Tenant Context

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/chat/service/ChatSessionManager.kt`**

```kotlin
// Update existing ChatSessionManager to use tenant context

@CachePut(
    cacheNames = [SovereignRagCache.CHAT_SESSION],
    key = "#result.sessionId"  // Remove tenant prefix from key
)
fun createNewChatSession(persona: String, language: String?): ChatSession {
    val tenantId = TenantContext.getCurrentTenant()

    val session = ChatSession(
        sessionId = UUID.randomUUID().toString(),
        persona = persona,
        language = language,
        conversationHistory = mutableListOf(),
        createdAt = LocalDateTime.now()
    )

    logger.info { "[$tenantId] Created new chat session: ${session.sessionId}" }
    return session
}

// Similar updates for other methods
```

**Deliverables:**
- Update ChatSessionManager with tenant context
- Update ConversationalAgentService to use new ContentService
- Remove tenant_id from Redis keys (implicit in database routing)

---

## Phase 6: WordPress Plugin Updates (Days 14-15)

### 6.1 Add Tenant Configuration

**File: `/wordpress-plugin/sovereign-rag-plugin/includes/admin-settings.php`**

```php
// Add new settings fields
add_settings_field(
    'sovereignrag_tenant_id',
    'Tenant ID',
    'sovereignrag_ai_tenant_id_callback',
    'sovereign-rag-settings',
    'sovereignrag_ai_section'
);

add_settings_field(
    'sovereignrag_api_key',
    'API Key',
    'sovereignrag_ai_api_key_callback',
    'sovereign-rag-settings',
    'sovereignrag_ai_section'
);

// Register settings
register_setting('sovereign-rag-settings', 'sovereignrag_tenant_id');
register_setting('sovereign-rag-settings', 'sovereignrag_api_key');

// Callback functions
function sovereignrag_ai_tenant_id_callback() {
    $tenant_id = get_option('sovereignrag_tenant_id', '');
    echo '<input type="text" name="sovereignrag_tenant_id" value="' . esc_attr($tenant_id) . '" class="regular-text" required />';
    echo '<p class="description">Your unique tenant identifier provided by Sovereign RAG.</p>';
}

function sovereignrag_ai_api_key_callback() {
    $api_key = get_option('sovereignrag_api_key', '');
    $display_key = $api_key ? substr($api_key, 0, 8) . '...' . substr($api_key, -4) : '';
    echo '<input type="password" name="sovereignrag_api_key" value="' . esc_attr($api_key) . '" class="regular-text" required />';
    echo '<p class="description">Your API key (will be stored securely). Currently: ' . esc_html($display_key) . '</p>';
}
```

### 6.2 Update Content Sync

**File: `/wordpress-plugin/sovereign-rag-plugin/includes/content-sync.php`**

```php
public function sync_post_to_graph($post_id, $post, $update) {
    // ... existing code ...

    // Get tenant credentials
    $tenant_id = get_option('sovereignrag_tenant_id');
    $api_key = get_option('sovereignrag_api_key');

    if (empty($tenant_id) || empty($api_key)) {
        error_log('Sovereign RAG: Missing tenant credentials. Please configure in settings.');
        return;
    }

    // Make API request with tenant headers
    $response = wp_remote_post($api_url . '/api/ingest', array(
        'headers' => array(
            'Content-Type' => 'application/json',
            'X-Tenant-ID' => $tenant_id,
            'X-API-Key' => $api_key
        ),
        'body' => json_encode($data),
        'timeout' => 30,
        'blocking' => false
    ));

    // ... error handling ...
}
```

### 6.3 Update Chat Widget

**File: `/wordpress-plugin/sovereign-rag-plugin/includes/chat-widget.php`**

```php
// Pass tenant credentials to JavaScript
wp_localize_script('sovereign-rag-chat-widget', 'sovereignragChat', array(
    'apiUrl' => esc_url($frontend_api_url),
    'tenantId' => get_option('sovereignrag_tenant_id'),
    'apiKey' => get_option('sovereignrag_api_key'),
    // ... other settings ...
));
```

**File: `/wordpress-plugin/sovereign-rag-plugin/assets/js/chat-widget.js`**

```javascript
// Add tenant headers to all API requests
function initializeAgentSession(firstMessage) {
    const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/start';

    $.ajax({
        url: apiUrl,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': sovereignragChat.tenantId,
            'X-API-Key': sovereignragChat.apiKey
        },
        data: JSON.stringify({
            persona: sovereignragChat.ragPersona || 'customer_service',
            language: responseLanguage
        }),
        // ... rest of the code ...
    });
}

// Apply same headers to all other AJAX calls:
// - sendAgentMessage
// - closeSession
// - fetchAutocomplete
// - submitSatisfactionFeedback
```

**Deliverables:**
- WordPress admin settings for tenant credentials
- Updated content sync with tenant headers
- Updated chat widget with tenant headers

---

## Phase 7: Data Migration from Neo4j (Days 16-17)

### 7.1 Migration Script

**File: `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/migration/Neo4jToPgVectorMigration.kt`**

```kotlin
package ai.sovereignrag.migration

import mu.KotlinLogging
import ai.sovereignrag.content.domain.Document
import ai.sovereignrag.content.repository.DocumentRepository
import ai.sovereignrag.tenant.context.TenantContext
import ai.sovereignrag.tenant.service.TenantRegistry
import org.neo4j.driver.Driver
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class Neo4jToPgVectorMigration(
    private val neo4jDriver: Driver,
    private val documentRepository: DocumentRepository,
    private val tenantRegistry: TenantRegistry
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (!args.contains("--migrate")) {
            return  // Only run if explicitly requested
        }

        logger.info { "Starting migration from Neo4j to PostgreSQL..." }

        try {
            // For single tenant migration, specify tenant ID
            val tenantId = args.find { it.startsWith("--tenant=") }
                ?.substringAfter("=")
                ?: "default-tenant"

            migrateDocuments(tenantId)

            logger.info { "Migration completed successfully!" }
        } catch (e: Exception) {
            logger.error(e) { "Migration failed" }
            throw e
        }
    }

    private fun migrateDocuments(tenantId: String) {
        logger.info { "Migrating documents for tenant: $tenantId" }

        // Set tenant context
        TenantContext.setCurrentTenant(tenantId)

        try {
            neo4jDriver.session().use { session ->
                val result = session.run("""
                    MATCH (d:Document)
                    RETURN d.id as id,
                           d.title as title,
                           d.content as content,
                           d.url as url,
                           d.source as source,
                           d.created_at as created_at,
                           d.metadata as metadata,
                           d.embedding as embedding
                """)

                var count = 0
                result.forEach { record ->
                    try {
                        val document = Document(
                            id = UUID.fromString(record.get("id").asString()),
                            title = record.get("title").asString(),
                            content = record.get("content").asString(),
                            url = record.get("url").asString(null),
                            source = record.get("source").asString(null),
                            embedding = record.get("embedding").asList { it.asFloat() }.toFloatArray(),
                            metadata = emptyMap(),  // Parse from JSON if needed
                            createdAt = LocalDateTime.parse(record.get("created_at").asString())
                        )

                        documentRepository.save(document)
                        count++

                        if (count % 100 == 0) {
                            logger.info { "Migrated $count documents..." }
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to migrate document: ${record.get("id")}" }
                    }
                }

                logger.info { "Migrated $count documents for tenant $tenantId" }
            }
        } finally {
            TenantContext.clear()
        }
    }
}
```

### 7.2 Migration Instructions

**Command to run migration:**
```bash
# Migrate to default tenant
java -jar core-ai.jar --migrate --tenant=default-tenant

# Migrate to specific tenant
java -jar core-ai.jar --migrate --tenant=example-com
```

**Manual migration steps:**
1. Export data from Neo4j to CSV
2. Create tenant in PostgreSQL
3. Import CSV to PostgreSQL
4. Generate embeddings if needed

**Deliverables:**
- Automated migration script
- Manual migration documentation
- Data validation scripts

---

## Phase 8: Testing & Validation (Days 18-20)

### 8.1 Unit Tests

```kotlin
@SpringBootTest
class ContentServicePgVectorTest {

    @Test
    fun `should ingest and retrieve document`() {
        // Set tenant context
        TenantContext.setCurrentTenant("test-tenant")

        // Ingest document
        val doc = ContentDocument(
            id = UUID.randomUUID().toString(),
            title = "Test Document",
            content = "This is test content",
            url = "https://test.com/page"
        )
        contentService.ingest(doc)

        // Search for it
        val results = contentService.search("test content", 5)

        assertTrue(results.isNotEmpty())
        assertEquals("Test Document", results.first().metadata["title"])
    }

    @Test
    fun `should isolate tenants`() {
        // Tenant A
        TenantContext.setCurrentTenant("tenant-a")
        contentService.ingest(ContentDocument(
            id = UUID.randomUUID().toString(),
            title = "Tenant A Doc",
            content = "Private content for A"
        ))

        // Tenant B
        TenantContext.setCurrentTenant("tenant-b")
        val results = contentService.search("Private content", 10)

        // Should not find Tenant A's document
        assertTrue(results.none { it.fact.contains("Private content for A") })
    }
}
```

### 8.2 Integration Tests

```kotlin
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TenantIsolationIntegrationTest {

    @Test
    fun `should enforce tenant isolation at API level`() {
        // Call API with Tenant A credentials
        val responseA = restTemplate.exchange(
            "/api/search",
            HttpMethod.POST,
            HttpEntity(searchRequest, headersForTenantA()),
            SearchResponse::class.java
        )

        // Call API with Tenant B credentials
        val responseB = restTemplate.exchange(
            "/api/search",
            HttpMethod.POST,
            HttpEntity(searchRequest, headersForTenantB()),
            SearchResponse::class.java
        )

        // Results should be different
        assertNotEquals(responseA.body.results, responseB.body.results)
    }
}
```

### 8.3 Performance Testing

```bash
# Load test with Apache Bench
ab -n 1000 -c 10 \
   -H "X-Tenant-ID: test-tenant" \
   -H "X-API-Key: test-key" \
   -T "application/json" \
   -p search-query.json \
   http://localhost:8000/api/search

# Vector search performance
EXPLAIN ANALYZE
SELECT * FROM documents
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;
```

**Deliverables:**
- Unit tests for ContentService
- Integration tests for tenant isolation
- Performance benchmarks
- Load testing results

---

## Phase 9: Deployment & Rollout (Days 21-22)

### 9.1 Deployment Checklist

**Pre-Deployment:**
- [ ] Backup all Neo4j data
- [ ] Backup WordPress database
- [ ] Test rollback procedure
- [ ] Configure PostgreSQL production settings
- [ ] Set up monitoring (Datadog, Prometheus, etc.)
- [ ] Configure alerts for failed queries

**Deployment Steps:**
1. Deploy PostgreSQL server
2. Create master database
3. Deploy updated backend (with feature flag for Neo4j/pgvector)
4. Create first tenant (migrate existing data)
5. Update WordPress plugin
6. Test end-to-end
7. Switch traffic to pgvector
8. Monitor for issues
9. Decommission Neo4j (after 1 week)

### 9.2 Feature Flag Configuration

```kotlin
@ConfigurationProperties(prefix = "sovereignrag.storage")
data class StorageConfig(
    val backend: StorageBackend = StorageBackend.PGVECTOR
)

enum class StorageBackend {
    NEO4J,      // Old
    PGVECTOR    // New
}

@Service
class ContentServiceRouter(
    private val neo4jService: ContentServiceNeo4j,
    private val pgvectorService: ContentServicePgVector,
    private val config: StorageConfig
) {
    fun search(query: String, limit: Int): List<SearchResult> {
        return when (config.backend) {
            StorageBackend.NEO4J -> neo4jService.search(query, limit)
            StorageBackend.PGVECTOR -> pgvectorService.search(query, limit)
        }
    }
}
```

### 9.3 Monitoring Setup

```yaml
# Prometheus metrics
spring:
  metrics:
    export:
      prometheus:
        enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: sovereign-rag
    enable:
      jvm: true
      process: true
      hikaricp: true
```

**Custom metrics:**
```kotlin
@Component
class TenantMetrics(
    private val meterRegistry: MeterRegistry
) {
    fun recordSearch(tenantId: String, durationMs: Long) {
        meterRegistry.timer("tenant.search.duration",
            "tenant", tenantId
        ).record(durationMs, TimeUnit.MILLISECONDS)
    }

    fun recordIngestion(tenantId: String) {
        meterRegistry.counter("tenant.ingestion.count",
            "tenant", tenantId
        ).increment()
    }
}
```

**Deliverables:**
- Deployment runbook
- Feature flag configuration
- Monitoring dashboards
- Alert rules

---

## Phase 10: Documentation & Training (Day 23)

### 10.1 Technical Documentation

**Create docs:**
- Architecture diagram (Neo4j → PostgreSQL)
- Multi-tenant data flow
- API authentication guide
- Tenant onboarding guide
- Troubleshooting guide

### 10.2 WordPress Plugin Setup Guide

```markdown
# Sovereign RAG Plugin Setup Guide

## Installation

1. Install plugin from WordPress admin
2. Go to Settings → Sovereign RAG
3. Contact SovereignRag to get your tenant credentials:
   - Tenant ID
   - API Key
4. Enter credentials in plugin settings
5. Save settings
6. Plugin will automatically sync your content

## Tenant Creation (Admin Only)

curl -X POST http://localhost:8000/api/admin/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "example-com",
    "name": "Example.com",
    "contactEmail": "admin@example.com",
    "wordpressUrl": "https://example.com"
  }'

Response:
{
  "tenantId": "example-com",
  "apiKey": "xyzabc123...",  // Save this!
  "databaseName": "sovereignrag_tenant_example_com",
  "message": "Tenant created successfully..."
}
```

**Deliverables:**
- Technical architecture docs
- API documentation
- WordPress plugin setup guide
- Admin guide for tenant management

---

## Rollback Plan

If issues arise during migration:

### Quick Rollback (< 1 hour)
1. Change feature flag: `sovereignrag.storage.backend=NEO4J`
2. Restart backend
3. Neo4j is still running with old data
4. WordPress plugin works with both backends

### Full Rollback (if PostgreSQL has issues)
1. Stop ingesting to PostgreSQL
2. Switch all traffic to Neo4j
3. Investigate PostgreSQL issues
4. Fix and retry migration

---

## Success Criteria

- [ ] All existing functionality works with pgvector
- [ ] Tenant isolation is enforced (verified by tests)
- [ ] Search performance is equal or better than Neo4j
- [ ] Zero data loss during migration
- [ ] WordPress plugin works with new backend
- [ ] Monitoring shows healthy metrics
- [ ] Documentation is complete

---

## Timeline Summary

| Phase | Days | Description |
|-------|------|-------------|
| 0. Preparation | 1-2 | Install PostgreSQL, pgvector |
| 1. Schema Design | 3-4 | Create master + tenant schemas |
| 2. Infrastructure | 5-7 | Tenant context, datasource routing |
| 3. Tenant Management | 8-9 | Tenant registry, API |
| 4. Content Service | 10-12 | Migrate to pgvector |
| 5. Chat Services | 13 | Update with tenant context |
| 6. WordPress Plugin | 14-15 | Add tenant authentication |
| 7. Data Migration | 16-17 | Migrate from Neo4j |
| 8. Testing | 18-20 | Unit, integration, performance |
| 9. Deployment | 21-22 | Production rollout |
| 10. Documentation | 23 | Docs and training |

**Total: ~3 weeks (23 days)**

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Data loss during migration | Backup before migration, validate after |
| Performance degradation | Benchmark before/after, tune PostgreSQL |
| Tenant isolation breach | Comprehensive tests, security audit |
| WordPress plugin breaks | Feature flag, gradual rollout |
| PostgreSQL downtime | High availability setup, replicas |

---

## Next Steps

After completing this migration plan:

1. **Review and approve** this plan with stakeholders
2. **Set up development environment** (Phase 0)
3. **Begin implementation** following the phases
4. **Regular checkpoints** after each phase
5. **Testing at each milestone** before proceeding
6. **Gradual rollout** to production with monitoring

---

**Document Version**: 1.0
**Date**: 2025-10-28
**Author**: Sovereign RAG Migration Team
