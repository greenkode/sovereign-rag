package ai.sovereignrag.audit.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(ResourceServerJwtProperties::class)
class ResourceServerJwtConfig(
    private val jwtProperties: ResourceServerJwtProperties
) {

    @Bean
    fun rsaPublicKey(): RSAPublicKey {
        return try {
            val publicKeyContent = jwtProperties.publicKeyPath
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")

            val decoded = Base64.getDecoder().decode(publicKeyContent)
            val keySpec = X509EncodedKeySpec(decoded)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(keySpec) as RSAPublicKey
        } catch (e: Exception) {
            log.error(e) { "Failed to parse RSA public key from content" }
            throw IllegalStateException("Failed to parse RSA public key", e)
        }
    }
}
