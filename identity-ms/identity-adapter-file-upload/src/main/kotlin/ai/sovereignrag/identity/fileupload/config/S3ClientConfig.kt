package ai.sovereignrag.identity.fileupload.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
@EnableConfigurationProperties(S3ConfigProperties::class)
class S3ClientConfig(
    private val properties: S3ConfigProperties
) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            properties.accessKey,
            properties.secretKey
        )

        return S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(
            properties.accessKey,
            properties.secretKey
        )

        val serviceConfig = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build()

        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .serviceConfiguration(serviceConfig)
            .build()
    }
}
