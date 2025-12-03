package ai.sovereignrag.commons.license

import ai.sovereignrag.commons.subscription.SubscriptionTier
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "license.online")
data class OnlineLicenseProperties(
    val enabled: Boolean = false,
    val serverUrl: String = "http://localhost:9085",
    val timeout: Long = 5000,
    val deploymentId: String? = null,
    val hostname: String? = null,
    val applicationVersion: String? = null
)

@Service
@ConditionalOnProperty(prefix = "license.online", name = ["enabled"], havingValue = "true")
class OnlineLicenseVerificationService(
    private val onlineLicenseProperties: OnlineLicenseProperties,
    private val offlineValidator: LicenseValidator
) {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(onlineLicenseProperties.serverUrl)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun verifyLicenseOnline(licenseKey: String): LicenseInfo {
        return try {
            val request = OnlineVerificationRequest(
                licenseKey = licenseKey,
                deploymentId = onlineLicenseProperties.deploymentId,
                hostname = onlineLicenseProperties.hostname,
                applicationVersion = onlineLicenseProperties.applicationVersion,
                metadata = null
            )

            val response = webClient.post()
                .uri("/api/v1/verify")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OnlineVerificationResponse::class.java)
                .timeout(Duration.ofMillis(onlineLicenseProperties.timeout))
                .onErrorResume { error ->
                    log.warn(error) { "Online verification failed, falling back to offline" }
                    Mono.empty()
                }
                .block()

            response?.let { mapResponseToLicenseInfo(it, licenseKey) }
                ?: run {
                    log.info { "Online verification unavailable, using offline validation" }
                    offlineValidator.validateLicense(licenseKey)
                }
        } catch (e: WebClientResponseException) {
            log.warn { "Online verification error: ${e.statusCode} - ${e.responseBodyAsString}" }
            offlineValidator.validateLicense(licenseKey)
        } catch (e: Exception) {
            log.error(e) { "Online verification failed" }
            offlineValidator.validateLicense(licenseKey)
        }
    }

    fun reportUsage(
        licenseKey: String,
        customerId: String,
        tokensUsed: Long,
        activeKnowledgeBases: Int,
        activeUsers: Int,
        apiCalls: Long
    ) {
        try {
            val request = UsageReportRequest(
                licenseKey = licenseKey,
                customerId = customerId,
                deploymentId = onlineLicenseProperties.deploymentId,
                tokensUsed = tokensUsed,
                activeKnowledgeBases = activeKnowledgeBases,
                activeUsers = activeUsers,
                apiCalls = apiCalls,
                metadata = null
            )

            webClient.post()
                .uri("/api/v1/verify/usage")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UsageReportResponse::class.java)
                .timeout(Duration.ofMillis(onlineLicenseProperties.timeout))
                .doOnError { error ->
                    log.warn(error) { "Failed to report usage to license server" }
                }
                .subscribe { response ->
                    log.debug { "Usage reported: ${response.message}" }
                }
        } catch (e: Exception) {
            log.warn(e) { "Could not report usage to license server" }
        }
    }

    private fun mapResponseToLicenseInfo(
        response: OnlineVerificationResponse,
        licenseKey: String
    ): LicenseInfo {
        return LicenseInfo(
            licenseKey = licenseKey,
            customerId = response.customerId,
            customerName = response.customerName,
            tier = response.tier,
            maxTokensPerMonth = response.maxTokensPerMonth,
            maxKnowledgeBases = response.maxKnowledgeBases,
            features = response.features,
            issuedAt = response.issuedAt,
            expiresAt = response.expiresAt,
            isValid = response.valid,
            validationMessage = response.message
        )
    }
}

data class OnlineVerificationRequest(
    val licenseKey: String,
    val deploymentId: String?,
    val hostname: String?,
    val applicationVersion: String?,
    val metadata: Map<String, Any>?
)

data class OnlineVerificationResponse(
    val valid: Boolean,
    val message: String?,
    val customerId: String,
    val customerName: String,
    val tier: SubscriptionTier,
    val maxTokensPerMonth: Long,
    val maxKnowledgeBases: Int,
    val features: Set<LicenseFeature>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val revoked: Boolean
)

data class UsageReportRequest(
    val licenseKey: String,
    val customerId: String,
    val deploymentId: String?,
    val tokensUsed: Long,
    val activeKnowledgeBases: Int,
    val activeUsers: Int,
    val apiCalls: Long,
    val metadata: Map<String, Any>?
)

data class UsageReportResponse(
    val success: Boolean,
    val message: String
)
