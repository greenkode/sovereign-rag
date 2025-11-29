package ai.sovereignrag.identity.fileupload.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage.s3")
data class S3ConfigProperties(
    val endpoint: String = "http://localhost:9000",
    val region: String = "us-east-1",
    val accessKey: String = "",
    val secretKey: String = "",
    val bucket: String = "sovereign-rag",
    val publicUrl: String? = null
)
