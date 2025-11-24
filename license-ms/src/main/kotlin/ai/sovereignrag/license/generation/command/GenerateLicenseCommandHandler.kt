package ai.sovereignrag.license.generation.command

import ai.sovereignrag.license.domain.ClientStatus
import ai.sovereignrag.license.domain.License
import ai.sovereignrag.license.domain.LicenseTier
import ai.sovereignrag.license.repository.ClientRepository
import ai.sovereignrag.license.repository.LicenseRepository
import ai.sovereignrag.license.service.LicenseGenerationRequest
import ai.sovereignrag.license.service.LicenseGenerationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class GenerateLicenseCommandHandler(
    private val licenseGenerationService: LicenseGenerationService,
    private val licenseRepository: LicenseRepository,
    private val clientRepository: ClientRepository
) {

    @Transactional
    fun handle(command: GenerateLicenseCommand): GenerateLicenseResult {
        log.info { "Generating license for client: ${command.clientId}" }

        val client = clientRepository.findByClientId(command.clientId)
            ?.takeIf { it.status == ClientStatus.ACTIVE }
            ?: return GenerateLicenseResult(
                success = false,
                licenseKey = null,
                clientId = null,
                tier = null,
                expiresAt = null,
                error = "Client not found or inactive"
            )

        val generationRequest = LicenseGenerationRequest(
            clientId = command.clientId,
            clientName = client.clientName,
            tier = command.tier,
            maxTokensPerMonth = command.maxTokensPerMonth,
            maxTenants = command.maxTenants,
            features = command.features,
            expiresAt = command.expiresAt,
            privateKey = command.privateKey
        )

        val licenseKey = licenseGenerationService.generateLicenseKey(generationRequest)

        val license = License(
            licenseKey = licenseKey,
            clientId = command.clientId,
            tier = LicenseTier.valueOf(command.tier),
            maxTokensPerMonth = command.maxTokensPerMonth,
            maxTenants = command.maxTenants,
            features = command.features.toTypedArray(),
            expiresAt = command.expiresAt,
            createdBy = "admin"
        )

        licenseRepository.save(license)

        log.info { "License generated and saved for ${command.clientId}" }

        return GenerateLicenseResult(
            success = true,
            licenseKey = licenseKey,
            clientId = command.clientId,
            tier = command.tier,
            expiresAt = command.expiresAt,
            error = null
        )
    }
}
