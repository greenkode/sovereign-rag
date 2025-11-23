package ai.sovereignrag.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class ResourceServerJwtProperties(
    var publicKeyPath: String = "",
    var issuer: String = "http://localhost:9093",
    var audience: String = "core-ms-client"
)
