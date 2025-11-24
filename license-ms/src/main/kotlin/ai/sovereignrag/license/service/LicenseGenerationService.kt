package ai.sovereignrag.license.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

private val log = KotlinLogging.logger {}

@Service
class LicenseGenerationService {

    fun generateLicenseKey(request: LicenseGenerationRequest): String {
        log.info { "Generating license for customer: ${request.customerId}" }

        val payload = buildPayload(request)
        val metadata = buildMetadata()

        val payloadBase64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))

        val metadataBase64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(metadata.toByteArray(StandardCharsets.UTF_8))

        val signature = signPayload(payload, request.privateKey)
        val signatureBase64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signature)

        val licenseKey = "$payloadBase64.$signatureBase64.$metadataBase64"

        log.info { "License generated successfully for ${request.customerId}" }
        return licenseKey
    }

    private fun buildPayload(request: LicenseGenerationRequest): String {
        return buildString {
            append("cid=${request.customerId}")
            append("&name=${request.customerName}")
            append("&tier=${request.tier}")
            append("&tokens=${request.maxTokensPerMonth}")
            append("&tenants=${request.maxTenants}")

            if (request.features.isNotEmpty()) {
                append("&features=${request.features.joinToString(",")}")
            }

            append("&iat=${Instant.now().epochSecond}")

            request.expiresAt?.let {
                append("&exp=${it.epochSecond}")
            }
        }
    }

    private fun buildMetadata(): String {
        return buildString {
            append("version=1")
            append("&issued_by=license-ms")
            append("&generated_at=${Instant.now().epochSecond}")
        }
    }

    private fun signPayload(payload: String, privateKeyPem: String): ByteArray {
        val privateKey = loadPrivateKey(privateKeyPem)

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(payload.toByteArray(StandardCharsets.UTF_8))

        return signature.sign()
    }

    private fun loadPrivateKey(privateKeyPem: String): PrivateKey {
        val keyContent = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePrivate(keySpec)
    }

    fun generateKeyPair(): KeyPairResult {
        log.info { "Generating new RSA key pair" }

        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val privateKeyBytes = keyPair.private.encoded
        val publicKeyBytes = keyPair.public.encoded

        val privateKeyPem = buildString {
            appendLine("-----BEGIN PRIVATE KEY-----")
            appendLine(Base64.getEncoder().encodeToString(privateKeyBytes).chunked(64).joinToString("\n"))
            appendLine("-----END PRIVATE KEY-----")
        }

        val publicKeyPem = buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(Base64.getEncoder().encodeToString(publicKeyBytes).chunked(64).joinToString("\n"))
            appendLine("-----END PUBLIC KEY-----")
        }

        log.info { "Key pair generated successfully" }

        return KeyPairResult(
            privateKey = privateKeyPem,
            publicKey = publicKeyPem
        )
    }
}

data class LicenseGenerationRequest(
    val customerId: String,
    val customerName: String,
    val tier: String,
    val maxTokensPerMonth: Long,
    val maxTenants: Int,
    val features: List<String>,
    val expiresAt: Instant?,
    val privateKey: String
)

data class KeyPairResult(
    val privateKey: String,
    val publicKey: String
)
