package ai.sovereignrag.identity.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig(
    private val jwtProperties: JwtProperties
) {
    
    @Bean
    fun rsaPrivateKey(): RSAPrivateKey {
        return try {
            val privateKeyContent = jwtProperties.privateKeyPath
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
            
            val decoded = Base64.getDecoder().decode(privateKeyContent)
            val keySpec = PKCS8EncodedKeySpec(decoded)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePrivate(keySpec) as RSAPrivateKey
        } catch (e: Exception) {
            log.error(e) { "Failed to parse RSA private key from content" }
            throw IllegalStateException("Failed to parse RSA private key", e)
        }
    }
    
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
    
    @Bean
    fun jwkSource(rsaPublicKey: RSAPublicKey, rsaPrivateKey: RSAPrivateKey): JWKSource<SecurityContext?> {
        val rsaKey = RSAKey.Builder(rsaPublicKey)
            .privateKey(rsaPrivateKey)
            .keyID("bml-identity-key-1")
            .build()
        
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet<SecurityContext?>(jwkSet)
    }
    
    @Bean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext?>): JwtEncoder {
        return NimbusJwtEncoder(jwkSource)
    }
    
    @Bean
    @Primary
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext?>): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }
}