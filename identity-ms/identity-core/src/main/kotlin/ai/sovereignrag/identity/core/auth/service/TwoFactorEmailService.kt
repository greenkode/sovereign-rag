package ai.sovereignrag.identity.core.auth.service

import ai.sovereignrag.identity.commons.notification.MessagePayload
import ai.sovereignrag.identity.commons.notification.MessageRecipient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.integration.CoreMerchantClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class TwoFactorEmailService(
    private val coreMerchantClient: CoreMerchantClient
) {

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")
        private const val CODE_EXPIRY_MINUTES = "10 minutes"
        private val LOCAL_IP_PREFIXES = listOf("127.", "192.168.", "10.", "172.", "fc", "fd")
        private val IPV6_LOOPBACK = setOf("::1", "0:0:0:0:0:0:0:1")
    }

    fun sendTwoFactorCode(user: OAuthUser, code: String, ipAddress: String? = null) {
        val formattedTime = formatCurrentTime()
        val approximateLocation = resolveLocationFromIp(ipAddress)
        val displayIpAddress = normalizeIpForDisplay(ipAddress ?: "unknown")

        val messagePayload = MessagePayload(
            recipients = listOf(MessageRecipient(user.email, user.fullName())),
            templateName = "TWO_FACTOR_AUTH",
            channel = "EMAIL",
            priority = "HIGH",
            parameters = mapOf(
                "name" to user.fullName(),
                "verification_code" to code,
                "expiry_time" to CODE_EXPIRY_MINUTES,
                "request_time" to formattedTime,
                "ip_address" to displayIpAddress,
                "location" to approximateLocation
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString(),
            recipientType = "INDIVIDUAL"
        )

        runCatching { coreMerchantClient.sendMessage(messagePayload) }
            .onSuccess { log.info { "2FA code email sent to ${user.email}" } }
            .onFailure { e ->
                log.error(e) { "Failed to send 2FA code email to ${user.email}" }
                throw RuntimeException("Failed to send verification email", e)
            }
    }

    private fun formatCurrentTime(): String =
        Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DATE_TIME_FORMATTER)

    private fun resolveLocationFromIp(ipAddress: String?): String =
        ipAddress
            ?.takeIf { it.isNotBlank() && it != "unknown" }
            ?.let { ip ->
                when {
                    isLocalOrPrivateIp(ip) -> "Local network"
                    else -> "Unknown location"
                }
            }
            ?: "Unknown location"

    private fun isLocalOrPrivateIp(ipAddress: String): Boolean =
        LOCAL_IP_PREFIXES.any { ipAddress.startsWith(it) } ||
            IPV6_LOOPBACK.contains(ipAddress) ||
            ipAddress == "localhost"

    private fun normalizeIpForDisplay(ipAddress: String): String =
        ipAddress.takeUnless { IPV6_LOOPBACK.contains(it) } ?: "127.0.0.1"
}
