package ai.sovereignrag.ingestion.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val type: StorageType = StorageType.MINIO,
    val minio: MinioProperties = MinioProperties(),
    val s3: S3Properties = S3Properties()
)

enum class StorageType {
    MINIO, S3
}

data class MinioProperties(
    val endpoint: String = "http://localhost:9000",
    val accessKey: String = "minioadmin",
    val secretKey: String = "minioadmin",
    val bucket: String = "sovereign-rag"
)

data class S3Properties(
    val region: String = "us-east-1",
    val accessKey: String = "",
    val secretKey: String = "",
    val bucket: String = "sovereign-rag"
)

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(
    private val properties: StorageProperties
) {

    @Bean
    fun s3Client(): S3Client {
        return when (properties.type) {
            StorageType.MINIO -> createMinioClient()
            StorageType.S3 -> createS3Client()
        }
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        return when (properties.type) {
            StorageType.MINIO -> createMinioPresigner()
            StorageType.S3 -> createS3Presigner()
        }
    }

    @Bean
    fun storageBucket(): String {
        return when (properties.type) {
            StorageType.MINIO -> properties.minio.bucket
            StorageType.S3 -> properties.s3.bucket
        }
    }

    @Bean
    fun storageBaseUrl(): String {
        return when (properties.type) {
            StorageType.MINIO -> properties.minio.endpoint
            StorageType.S3 -> "https://${properties.s3.bucket}.s3.${properties.s3.region}.amazonaws.com"
        }
    }

    private fun createMinioClient(): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(properties.minio.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.minio.accessKey,
                        properties.minio.secretKey
                    )
                )
            )
            .forcePathStyle(true)
            .build()
    }

    private fun createS3Client(): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(properties.s3.region))

        if (properties.s3.accessKey.isNotBlank() && properties.s3.secretKey.isNotBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.s3.accessKey,
                        properties.s3.secretKey
                    )
                )
            )
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create())
        }

        return builder.build()
    }

    private fun createMinioPresigner(): S3Presigner {
        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.minio.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.minio.accessKey,
                        properties.minio.secretKey
                    )
                )
            )
            .build()
    }

    private fun createS3Presigner(): S3Presigner {
        val builder = S3Presigner.builder()
            .region(Region.of(properties.s3.region))

        if (properties.s3.accessKey.isNotBlank() && properties.s3.secretKey.isNotBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.s3.accessKey,
                        properties.s3.secretKey
                    )
                )
            )
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create())
        }

        return builder.build()
    }
}
