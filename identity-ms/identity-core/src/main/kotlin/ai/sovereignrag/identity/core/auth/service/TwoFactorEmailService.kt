package ai.sovereignrag.identity.core.auth.service

import ai.sovereignrag.identity.commons.notification.MessagePayload
import ai.sovereignrag.identity.commons.notification.MessageRecipient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.integration.CoreMerchantClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class TwoFactorEmailService(
    private val coreMerchantClient: CoreMerchantClient
) {

    fun sendTwoFactorCode(
        user: OAuthUser, 
        code: String, 
        ipAddress: String? = null
    ) {
        val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
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
                "expiry_time" to "10 minutes",
                "request_time" to formattedTime,
                "ip_address" to displayIpAddress,
                "location" to approximateLocation
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString(),
            recipientType = "INDIVIDUAL"
        )

        try {
            coreMerchantClient.sendMessage(messagePayload)
            log.info { "2FA code email sent to ${user.email}" }
        } catch (e: Exception) {
            log.error(e) { "Failed to send 2FA code email to ${user.email}" }
            throw RuntimeException("Failed to send verification email", e)
        }
    }

    private fun resolveLocationFromIp(ipAddress: String?): String {
        if (ipAddress.isNullOrBlank() || ipAddress == "unknown") {
            return "Unknown location"
        }

        // For now, return a simple location based on common IP patterns
        // In production, this could integrate with a GeoIP service
        return when {
            isLocalOrPrivateIp(ipAddress) -> "Local network"
            else -> throw RuntimeException("Unable to resolve location for IP address: $ipAddress")
        }
    }
    
    private fun isLocalOrPrivateIp(ipAddress: String): Boolean {
        return ipAddress.startsWith("127.") ||
               ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("10.") ||
               ipAddress.startsWith("172.") ||
               ipAddress == "::1" ||
               ipAddress == "0:0:0:0:0:0:0:1" ||
               ipAddress.startsWith("fc") || // IPv6 private range
               ipAddress.startsWith("fd") || // IPv6 private range
               ipAddress == "localhost"
    }
    
    private fun normalizeIpForDisplay(ipAddress: String): String {
        return when (ipAddress) {
            "::1", "0:0:0:0:0:0:0:1" -> "127.0.0.1" // Convert IPv6 loopback to IPv4 loopback for readability
            else -> ipAddress
        }
    }
}