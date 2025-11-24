package ai.sovereignrag.license.generation.command

import ai.sovereignrag.license.domain.CustomerStatus
import ai.sovereignrag.license.domain.License
import ai.sovereignrag.license.domain.LicenseTier
import ai.sovereignrag.license.repository.CustomerRepository
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
    private val customerRepository: CustomerRepository
) {

    @Transactional
    fun handle(command: GenerateLicenseCommand): GenerateLicenseResult {
        log.info { "Generating license for customer: ${command.customerId}" }

        val customer = customerRepository.findByCustomerId(command.customerId)
            ?.takeIf { it.status == CustomerStatus.ACTIVE }
            ?: return GenerateLicenseResult(
                success = false,
                licenseKey = null,
                customerId = null,
                tier = null,
                expiresAt = null,
                error = "Customer not found or inactive"
            )

        val generationRequest = LicenseGenerationRequest(
            customerId = command.customerId,
            customerName = customer.customerName,
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
            customerId = command.customerId,
            tier = LicenseTier.valueOf(command.tier),
            maxTokensPerMonth = command.maxTokensPerMonth,
            maxTenants = command.maxTenants,
            features = command.features.toTypedArray(),
            expiresAt = command.expiresAt,
            createdBy = "admin"
        )

        licenseRepository.save(license)

        log.info { "License generated and saved for ${command.customerId}" }

        return GenerateLicenseResult(
            success = true,
            licenseKey = licenseKey,
            customerId = command.customerId,
            tier = command.tier,
            expiresAt = command.expiresAt,
            error = null
        )
    }
}
