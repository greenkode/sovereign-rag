package ai.sovereignrag.identity.core.trusteddevice.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import java.security.MessageDigest

private val log = KotlinLogging.logger {}

@Service
class DeviceFingerprintService {

    fun generateFingerprint(
        userAgent: String?,
        ipAddress: String?,
        acceptLanguage: String? = null,
        acceptEncoding: String? = null
    ): String {
        return listOfNotNull(
            userAgent?.let { "ua:$it" },
            ipAddress?.let { "ip:${normalizeIpAddress(it)}" },
            acceptLanguage?.let { "lang:$it" },
            acceptEncoding?.let { "enc:$it" }
        ).joinToString("|")
    }

    fun generateFingerprintFromRequest(request: HttpServletRequest, ipAddress: String?): String =
        generateFingerprint(
            userAgent = request.getHeader("User-Agent"),
            ipAddress = ipAddress,
            acceptLanguage = request.getHeader("Accept-Language"),
            acceptEncoding = request.getHeader("Accept-Encoding")
        )

    fun hashFingerprint(fingerprint: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(fingerprint.toByteArray())
        return hashBytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun extractDeviceName(userAgent: String?): String =
        userAgent?.let { agent ->
            when {
                agent.contains("iPhone") -> "iPhone"
                agent.contains("iPad") -> "iPad"
                agent.contains("Android") -> extractAndroidModel(agent)
                agent.contains("Windows") -> "Windows PC"
                agent.contains("Macintosh") -> "Mac"
                agent.contains("Linux") -> "Linux PC"
                else -> "Unknown Device"
            }
        } ?: "Unknown Device"

    private fun extractAndroidModel(userAgent: String): String =
        Regex("\\(([^;]+);([^)]+)\\)").find(userAgent)
            ?.groupValues
            ?.getOrNull(2)
            ?.trim()
            ?: "Android Device"

    private fun normalizeIpAddress(ip: String): String =
        ip.contains('.')
            .let { isIpv4 ->
                if (isIpv4) ip.split('.').take(3).joinToString(".")
                else ip.split(':').take(4).joinToString(":")
            }
}
