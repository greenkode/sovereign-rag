package ai.sovereignrag.commons.license

import ai.sovereignrag.commons.subscription.SubscriptionTier
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Base64

private val log = KotlinLogging.logger {}

@Service
class LicenseValidator(
    @Value("\${sovereignrag.license.public-key}") private val publicKeyPem: String
) {

    fun validateLicense(licenseKey: String): LicenseInfo {
        if (licenseKey.isBlank()) {
            return createInvalidLicense(licenseKey, "License key is empty")
        }

        return try {
            val parts = licenseKey.split(".")
            if (parts.size != 3) {
                return createInvalidLicense(licenseKey, "Invalid license key format")
            }

            val payload = parts[0]
            val signature = parts[1]
            val metadata = parts[2]

            if (!verifySignature(payload, signature)) {
                return createInvalidLicense(licenseKey, "Invalid license signature")
            }

            parseLicensePayload(licenseKey, payload, metadata)
        } catch (e: Exception) {
            log.error(e) { "Failed to validate license" }
            createInvalidLicense(licenseKey, "License validation failed: ${e.message}")
        }
    }

    private fun verifySignature(payload: String, signature: String): Boolean {
        return try {
            val publicKey = loadPublicKey()
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(payload.toByteArray(StandardCharsets.UTF_8))

            val signatureBytes = Base64.getUrlDecoder().decode(signature)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            log.error(e) { "Signature verification failed" }
            false
        }
    }

    private fun loadPublicKey(): PublicKey {
        val keyContent = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    private fun parseLicensePayload(
        licenseKey: String,
        payload: String,
        metadata: String
    ): LicenseInfo {
        val decodedPayload = String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
        val decodedMetadata = String(Base64.getUrlDecoder().decode(metadata), StandardCharsets.UTF_8)

        val payloadMap = decodedPayload.split("&").associate {
            val (key, value) = it.split("=")
            key to value
        }

        val metadataMap = decodedMetadata.split("&").associate {
            val (key, value) = it.split("=")
            key to value
        }

        val customerId = payloadMap["cid"] ?: throw IllegalArgumentException("Missing customer ID")
        val customerName = payloadMap["name"] ?: "Unknown"
        val tier = SubscriptionTier.valueOf(payloadMap["tier"] ?: "TRIAL")
        val maxTokens = payloadMap["tokens"]?.toLongOrNull() ?: 0L
        val maxKnowledgeBases = payloadMap["knowledge_bases"]?.toIntOrNull() ?: 1
        val issuedAt = Instant.ofEpochSecond(payloadMap["iat"]?.toLongOrNull() ?: 0)
        val expiresAt = payloadMap["exp"]?.toLongOrNull()?.let { Instant.ofEpochSecond(it) }

        val features = payloadMap["features"]?.split(",")
            ?.mapNotNull {
                try {
                    LicenseFeature.valueOf(it.trim())
                } catch (e: Exception) {
                    null
                }
            }?.toSet() ?: emptySet()

        val isExpired = expiresAt?.let { it.isBefore(Instant.now()) } ?: false

        return LicenseInfo(
            licenseKey = licenseKey,
            customerId = customerId,
            customerName = customerName,
            tier = tier,
            maxTokensPerMonth = maxTokens,
            maxKnowledgeBases = maxKnowledgeBases,
            features = features,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            isValid = !isExpired,
            validationMessage = if (isExpired) "License has expired" else null
        )
    }

    private fun createInvalidLicense(licenseKey: String, message: String): LicenseInfo {
        log.warn { "Invalid license: $message" }
        return LicenseInfo(
            licenseKey = licenseKey,
            customerId = "unknown",
            customerName = "Unknown",
            tier = SubscriptionTier.TRIAL,
            maxTokensPerMonth = 100_000,
            maxKnowledgeBases = 1,
            features = emptySet(),
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(7 * 24 * 3600), // 7 days trial
            isValid = false,
            validationMessage = message
        )
    }
}
