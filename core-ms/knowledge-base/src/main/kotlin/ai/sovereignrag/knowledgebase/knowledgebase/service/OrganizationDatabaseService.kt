package ai.sovereignrag.knowledgebase.knowledgebase.service

import ai.sovereignrag.commons.regional.RegionalDataSourceRegistry
import ai.sovereignrag.commons.regional.RegionalDatabaseProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.sql.DriverManager
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class OrganizationDatabaseService(
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int,
    private val organizationDatabaseRegistry: OrganizationDatabaseRegistry,
    private val regionalDataSourceRegistry: RegionalDataSourceRegistry,
    private val regionalDatabaseProperties: RegionalDatabaseProperties
) {

    fun ensureOrganizationDatabaseExists(organizationId: UUID, regionCode: String? = null) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)

        if (organizationDatabaseRegistry.isDatabaseCreated(organizationId)) {
            log.debug { "Database already exists for organization: $organizationId" }
            return
        }

        val targetRegion = regionCode ?: regionalDatabaseProperties.defaultRegion
        log.info { "Creating database for organization: $organizationId in region: $targetRegion" }
        createPostgreSQLDatabase(databaseName, targetRegion)
        organizationDatabaseRegistry.markDatabaseCreated(organizationId, databaseName)
        log.info { "Database created successfully: $databaseName in region: $targetRegion" }
    }

    fun createKnowledgeBaseSchema(organizationId: UUID, schemaName: String, regionCode: String? = null) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        val targetRegion = regionCode ?: regionalDatabaseProperties.defaultRegion
        log.info { "Creating schema $schemaName in database $databaseName (region: $targetRegion)" }

        val dbUrl = getRegionalDatabaseUrl(targetRegion, databaseName)
        val regionConfig = regionalDatabaseProperties.regions[targetRegion]
        val username = regionConfig?.username ?: dbUsername
        val password = regionConfig?.password ?: dbPassword

        DriverManager.getConnection(dbUrl, username, password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
            }
        }

        applySchemaToDatabase(databaseName, schemaName, targetRegion)
        log.info { "Schema created and migrated: $schemaName in region: $targetRegion" }
    }

    private fun getRegionalDatabaseUrl(regionCode: String, databaseName: String): String {
        val regionConfig = regionalDatabaseProperties.regions[regionCode]
        return regionConfig?.url?.let { baseUrl ->
            baseUrl.substringBeforeLast("/") + "/$databaseName"
        } ?: "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
    }

    fun dropKnowledgeBaseSchema(organizationId: UUID, schemaName: String, regionCode: String? = null) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        val targetRegion = regionCode ?: regionalDatabaseProperties.defaultRegion
        log.warn { "Dropping schema $schemaName from database $databaseName (region: $targetRegion)" }

        val dbUrl = getRegionalDatabaseUrl(targetRegion, databaseName)
        val regionConfig = regionalDatabaseProperties.regions[targetRegion]
        val username = regionConfig?.username ?: dbUsername
        val password = regionConfig?.password ?: dbPassword

        DriverManager.getConnection(dbUrl, username, password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DROP SCHEMA IF EXISTS $schemaName CASCADE")
            }
        }
    }

    fun migrateAllSchemas(organizationId: UUID, schemas: List<String>, regionCode: String? = null) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        val targetRegion = regionCode ?: regionalDatabaseProperties.defaultRegion
        log.info { "Migrating ${schemas.size} schemas in database $databaseName (region: $targetRegion)" }

        var successCount = 0
        var failureCount = 0

        schemas.forEach { schema ->
            runCatching {
                applySchemaToDatabase(databaseName, schema, targetRegion)
                successCount++
            }.onFailure { e ->
                log.error(e) { "Failed to migrate schema: $schema" }
                failureCount++
            }
        }

        log.info { "Migration complete: $successCount succeeded, $failureCount failed" }
    }

    private fun createPostgreSQLDatabase(databaseName: String, regionCode: String) {
        log.info { "Creating PostgreSQL database: $databaseName in region: $regionCode" }

        val regionConfig = regionalDatabaseProperties.regions[regionCode]
        val baseUrl = regionConfig?.url?.substringBeforeLast("/")
            ?: "jdbc:postgresql://$dbHost:$dbPort"
        val username = regionConfig?.username ?: dbUsername
        val password = regionConfig?.password ?: dbPassword

        val serverUrl = "$baseUrl/postgres"
        DriverManager.getConnection(serverUrl, username, password).use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
                val exists = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '$databaseName'"
                ).use { rs -> rs.next() }

                if (!exists) {
                    stmt.execute("""
                        CREATE DATABASE $databaseName
                        WITH OWNER = $username
                             ENCODING = 'UTF8'
                             LC_COLLATE = 'en_US.UTF-8'
                             LC_CTYPE = 'en_US.UTF-8'
                             TEMPLATE = template0
                    """)
                    log.info { "Database created: $databaseName in region: $regionCode" }
                } else {
                    log.info { "Database already exists: $databaseName" }
                }
            }
        }

        val dbUrl = "$baseUrl/$databaseName"
        DriverManager.getConnection(dbUrl, username, password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS btree_gin")
            }
        }

        log.info { "Database ready with extensions: $databaseName in region: $regionCode" }
    }

    private fun applySchemaToDatabase(databaseName: String, schemaName: String, regionCode: String? = null) {
        val targetRegion = regionCode ?: regionalDatabaseProperties.defaultRegion
        log.info { "Applying schema migrations to: $databaseName.$schemaName (region: $targetRegion)" }

        val dbUrl = getRegionalDatabaseUrl(targetRegion, databaseName)
        val regionConfig = regionalDatabaseProperties.regions[targetRegion]
        val username = regionConfig?.username ?: dbUsername
        val password = regionConfig?.password ?: dbPassword

        val flyway = Flyway.configure()
            .dataSource(dbUrl, username, password)
            .schemas(schemaName)
            .locations("classpath:db/kb-schema")
            .baselineOnMigrate(true)
            .placeholderReplacement(false)
            .load()

        val result = flyway.migrate()
        log.info { "Applied ${result.migrationsExecuted} migrations to $databaseName.$schemaName (region: $targetRegion)" }
    }
}

interface OrganizationDatabaseRegistry {
    fun getDatabaseName(organizationId: UUID): String
    fun isDatabaseCreated(organizationId: UUID): Boolean
    fun markDatabaseCreated(organizationId: UUID, databaseName: String)
}
