package ai.sovereignrag.commons.license

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

private val log = KotlinLogging.logger {}

@Configuration
class LicenseConfiguration(
    private val licenseValidator: LicenseValidator,
    @Value("\${sovereignrag.license.key:}") private val licenseKey: String,
    @Value("\${sovereignrag.license.strict:false}") private val strictMode: Boolean
) {

    @Autowired(required = false)
    private var onlineVerificationService: OnlineLicenseVerificationService? = null

    private lateinit var licenseInfo: LicenseInfo

    @EventListener(ApplicationReadyEvent::class)
    fun validateLicenseOnStartup() {
        log.info { "Validating SovereignRAG license..." }

        if (licenseKey.isBlank()) {
            log.warn { "No license key provided. Running in trial mode." }
            licenseInfo = createTrialLicense()
            printLicenseInfo()
            return
        }

        licenseInfo = onlineVerificationService?.let {
            log.info { "Using online license verification" }
            it.verifyLicenseOnline(licenseKey)
        } ?: run {
            log.info { "Using offline license verification" }
            licenseValidator.validateLicense(licenseKey)
        }

        if (!licenseInfo.isValid) {
            log.error { "Invalid license: ${licenseInfo.validationMessage}" }
            if (strictMode) {
                throw LicenseException("Invalid or expired license. Application cannot start in strict mode.")
            } else {
                log.warn { "Running with invalid license in non-strict mode. Limited functionality." }
            }
        } else {
            log.info { "License validated successfully for ${licenseInfo.customerName}" }
            printLicenseInfo()
        }
    }

    fun getLicenseInfo(): LicenseInfo {
        return if (::licenseInfo.isInitialized) {
            licenseInfo
        } else {
            createTrialLicense()
        }
    }

    private fun printLicenseInfo() {
        log.info {
            """

            ╔════════════════════════════════════════════════════════════════╗
            ║              SovereignRAG License Information                  ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Customer:        ${licenseInfo.customerName.padEnd(42)}║
            ║ Tier:            ${licenseInfo.tier.name.padEnd(42)}║
            ║ Max Tokens:      ${formatNumber(licenseInfo.maxTokensPerMonth).padEnd(42)}║
            ║ Max Tenants:     ${licenseInfo.maxTenants.toString().padEnd(42)}║
            ║ Status:          ${(if (licenseInfo.isValid) "Valid" else "Invalid").padEnd(42)}║
            ║ Expires:         ${formatExpiry(licenseInfo.expiresAt).padEnd(42)}║
            ║ Features:        ${licenseInfo.features.size.toString().padEnd(42)}║
            ╚════════════════════════════════════════════════════════════════╝
            """.trimIndent()
        }

        if (licenseInfo.features.isNotEmpty()) {
            log.info { "Enabled features: ${licenseInfo.features.joinToString(", ")}" }
        }
    }

    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000_000 -> "${number / 1_000_000_000}B"
            number >= 1_000_000 -> "${number / 1_000_000}M"
            number >= 1_000 -> "${number / 1_000}K"
            else -> number.toString()
        }
    }

    private fun formatExpiry(expiry: java.time.Instant?): String {
        return expiry?.toString() ?: "Never"
    }

    private fun createTrialLicense(): LicenseInfo {
        return LicenseInfo(
            licenseKey = "trial",
            customerId = "trial",
            customerName = "Trial User",
            tier = LicenseTier.TRIAL,
            maxTokensPerMonth = 100_000,
            maxTenants = 1,
            features = setOf(LicenseFeature.MULTI_TENANT),
            issuedAt = java.time.Instant.now(),
            expiresAt = java.time.Instant.now().plusSeconds(7 * 24 * 3600),
            isValid = true,
            validationMessage = "Trial license (7 days)"
        )
    }
}

class LicenseException(message: String) : RuntimeException(message)
