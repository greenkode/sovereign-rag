package ai.sovereignrag.commons.regional

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "regional-databases")
data class RegionalDatabaseProperties(
    val regions: Map<String, RegionDatabaseConfig> = emptyMap(),
    val defaultRegion: String = "eu-west-1"
)

data class RegionDatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val readReplicaUrl: String? = null,
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val connectionTimeout: Long = 30000,
    val idleTimeout: Long = 600000,
    val maxLifetime: Long = 1800000
)
