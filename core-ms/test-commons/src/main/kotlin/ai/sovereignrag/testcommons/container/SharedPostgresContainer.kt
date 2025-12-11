package ai.sovereignrag.testcommons.container

import org.testcontainers.containers.PostgreSQLContainer

object SharedPostgresContainer {

    val instance: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("sovereign_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .also { it.start() }
    }

    val jdbcUrl: String get() = instance.jdbcUrl
    val username: String get() = instance.username
    val password: String get() = instance.password
    val host: String get() = instance.host
    val port: Int get() = instance.firstMappedPort
}
