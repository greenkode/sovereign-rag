package ai.sovereignrag.knowledgebase.knowledgebase.service

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
    private val organizationDatabaseRegistry: OrganizationDatabaseRegistry
) {

    fun ensureOrganizationDatabaseExists(organizationId: UUID) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)

        if (organizationDatabaseRegistry.isDatabaseCreated(organizationId)) {
            log.debug { "Database already exists for organization: $organizationId" }
            return
        }

        log.info { "Creating database for organization: $organizationId" }
        createPostgreSQLDatabase(databaseName)
        organizationDatabaseRegistry.markDatabaseCreated(organizationId, databaseName)
        log.info { "Database created successfully: $databaseName" }
    }

    fun createKnowledgeBaseSchema(organizationId: UUID, schemaName: String) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        log.info { "Creating schema $schemaName in database $databaseName" }

        val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
        DriverManager.getConnection(dbUrl, dbUsername, dbPassword).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
            }
        }

        applySchemaToDatabase(databaseName, schemaName)
        log.info { "Schema created and migrated: $schemaName" }
    }

    fun dropKnowledgeBaseSchema(organizationId: UUID, schemaName: String) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        log.warn { "Dropping schema $schemaName from database $databaseName" }

        val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
        DriverManager.getConnection(dbUrl, dbUsername, dbPassword).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DROP SCHEMA IF EXISTS $schemaName CASCADE")
            }
        }
    }

    fun migrateAllSchemas(organizationId: UUID, schemas: List<String>) {
        val databaseName = organizationDatabaseRegistry.getDatabaseName(organizationId)
        log.info { "Migrating ${schemas.size} schemas in database $databaseName" }

        var successCount = 0
        var failureCount = 0

        schemas.forEach { schema ->
            runCatching {
                applySchemaToDatabase(databaseName, schema)
                successCount++
            }.onFailure { e ->
                log.error(e) { "Failed to migrate schema: $schema" }
                failureCount++
            }
        }

        log.info { "Migration complete: $successCount succeeded, $failureCount failed" }
    }

    private fun createPostgreSQLDatabase(databaseName: String) {
        log.info { "Creating PostgreSQL database: $databaseName" }

        val serverUrl = "jdbc:postgresql://$dbHost:$dbPort/postgres"
        DriverManager.getConnection(serverUrl, dbUsername, dbPassword).use { conn ->
            conn.autoCommit = true
            conn.createStatement().use { stmt ->
                val exists = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '$databaseName'"
                ).use { rs -> rs.next() }

                if (!exists) {
                    stmt.execute("""
                        CREATE DATABASE $databaseName
                        WITH OWNER = $dbUsername
                             ENCODING = 'UTF8'
                             LC_COLLATE = 'en_US.UTF-8'
                             LC_CTYPE = 'en_US.UTF-8'
                             TEMPLATE = template0
                    """)
                    log.info { "Database created: $databaseName" }
                } else {
                    log.info { "Database already exists: $databaseName" }
                }
            }
        }

        val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$databaseName"
        DriverManager.getConnection(dbUrl, dbUsername, dbPassword).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm")
                stmt.execute("CREATE EXTENSION IF NOT EXISTS btree_gin")
            }
        }

        log.info { "Database ready with extensions: $databaseName" }
    }

    private fun applySchemaToDatabase(databaseName: String, schemaName: String) {
        log.info { "Applying schema migrations to: $databaseName.$schemaName" }

        val flyway = Flyway.configure()
            .dataSource(
                "jdbc:postgresql://$dbHost:$dbPort/$databaseName",
                dbUsername,
                dbPassword
            )
            .schemas(schemaName)
            .locations("classpath:db/kb-schema")
            .baselineOnMigrate(true)
            .placeholderReplacement(false)
            .load()

        val result = flyway.migrate()
        log.info { "Applied ${result.migrationsExecuted} migrations to $databaseName.$schemaName" }
    }
}

interface OrganizationDatabaseRegistry {
    fun getDatabaseName(organizationId: UUID): String
    fun isDatabaseCreated(organizationId: UUID): Boolean
    fun markDatabaseCreated(organizationId: UUID, databaseName: String)
}
