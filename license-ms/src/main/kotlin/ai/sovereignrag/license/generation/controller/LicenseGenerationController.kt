package ai.sovereignrag.license.generation.controller

import ai.sovereignrag.license.generation.command.GenerateLicenseCommand
import ai.sovereignrag.license.generation.command.GenerateLicenseCommandHandler
import ai.sovereignrag.license.generation.query.GenerateKeyPairQuery
import ai.sovereignrag.license.generation.query.GenerateKeyPairQueryHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/licenses")
class LicenseGenerationController(
    private val generateLicenseCommandHandler: GenerateLicenseCommandHandler,
    private val generateKeyPairQueryHandler: GenerateKeyPairQueryHandler
) {

    @PostMapping("/generate")
    fun generateLicense(@RequestBody request: GenerateLicenseApiRequest): ResponseEntity<GenerateLicenseApiResponse> {
        log.info { "License generation request for customer: ${request.clientId}" }

        val command = GenerateLicenseCommand(
            clientId = request.clientId,
            tier = request.tier,
            maxTokensPerMonth = request.maxTokensPerMonth,
            maxTenants = request.maxTenants,
            features = request.features,
            expiresAt = request.expiresAt,
            privateKey = request.privateKey
        )

        val result = generateLicenseCommandHandler.handle(command)

        if (!result.success) {
            return ResponseEntity.badRequest().body(
                GenerateLicenseApiResponse(
                    success = false,
                    error = result.error
                )
            )
        }

        return ResponseEntity.ok(
            GenerateLicenseApiResponse(
                success = true,
                licenseKey = result.licenseKey,
                clientId = result.clientId,
                tier = result.tier,
                expiresAt = result.expiresAt
            )
        )
    }

    @PostMapping("/generate-keys")
    fun generateKeys(): ResponseEntity<GenerateKeyPairApiResponse> {
        log.info { "Generating new RSA key pair" }

        val query = GenerateKeyPairQuery()
        val result = generateKeyPairQueryHandler.handle(query)

        return ResponseEntity.ok(
            GenerateKeyPairApiResponse(
                publicKey = result.publicKey,
                privateKey = result.privateKey
            )
        )
    }
}

data class GenerateLicenseApiRequest(
    val clientId: String,
    val tier: String,
    val maxTokensPerMonth: Long,
    val maxTenants: Int,
    val features: List<String>,
    val expiresAt: Instant?,
    val privateKey: String
)

data class GenerateLicenseApiResponse(
    val success: Boolean,
    val licenseKey: String? = null,
    val clientId: String? = null,
    val tier: String? = null,
    val expiresAt: Instant? = null,
    val error: String? = null
)

data class GenerateKeyPairApiResponse(
    val publicKey: String,
    val privateKey: String
)
