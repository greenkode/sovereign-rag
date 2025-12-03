package ai.sovereignrag.commons.license

import ai.sovereignrag.commons.subscription.SubscriptionTier
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
        val contentWidth = 64
        val labelWidth = 18
        val valueWidth = contentWidth - labelWidth
        val border = "═".repeat(contentWidth)
        val title = "SovereignRAG License Information"
        val centeredTitle = title.padStart((contentWidth + title.length) / 2).padEnd(contentWidth)

        fun row(label: String, value: String) = "║ ${label.padEnd(labelWidth - 1)}${value.padEnd(valueWidth)}║"

        log.info {
            """

            ╔$border╗
            ║$centeredTitle║
            ╠$border╣
            ${row("Customer:", licenseInfo.customerName)}
            ${row("Tier:", licenseInfo.tier.name)}
            ${row("Max Tokens:", formatNumber(licenseInfo.maxTokensPerMonth))}
            ${row("Max KBs:", licenseInfo.maxKnowledgeBases.toString())}
            ${row("Status:", if (licenseInfo.isValid) "Valid" else "Invalid")}
            ${row("Expires:", formatExpiry(licenseInfo.expiresAt))}
            ${row("Features:", licenseInfo.features.size.toString())}
            ╚$border╝
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
            tier = SubscriptionTier.TRIAL,
            maxTokensPerMonth = 100_000,
            maxKnowledgeBases = 1,
            features = setOf(LicenseFeature.MULTI_KNOWLEDGE_BASE),
            issuedAt = java.time.Instant.now(),
            expiresAt = java.time.Instant.now().plusSeconds(30 * 24 * 3600),
            isValid = true,
            validationMessage = "Trial license (30 days)"
        )
    }
}

class LicenseException(message: String) : RuntimeException(message)
