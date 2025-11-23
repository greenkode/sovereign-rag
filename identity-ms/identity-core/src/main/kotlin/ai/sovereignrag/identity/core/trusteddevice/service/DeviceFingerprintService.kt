package ai.sovereignrag.identity.core.trusteddevice.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.security.MessageDigest
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@Service
class DeviceFingerprintService {

    fun generateFingerprint(
        userAgent: String?,
        ipAddress: String?,
        acceptLanguage: String? = null,
        acceptEncoding: String? = null
    ): String {
        val components = mutableListOf<String>()

        userAgent?.let { components.add("ua:$it") }
        ipAddress?.let { components.add("ip:${normalizeIpAddress(it)}") }
        acceptLanguage?.let { components.add("lang:$it") }
        acceptEncoding?.let { components.add("enc:$it") }

        return components.joinToString("|")
    }

    fun generateFingerprintFromRequest(request: HttpServletRequest, ipAddress: String?): String {
        return generateFingerprint(
            userAgent = request.getHeader("User-Agent"),
            ipAddress = ipAddress,
            acceptLanguage = request.getHeader("Accept-Language"),
            acceptEncoding = request.getHeader("Accept-Encoding")
        )
    }

    fun hashFingerprint(fingerprint: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(fingerprint.toByteArray())
        return hashBytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun extractDeviceName(userAgent: String?): String? {
        if (userAgent == null) return null

        return when {
            userAgent.contains("iPhone") -> "iPhone"
            userAgent.contains("iPad") -> "iPad"
            userAgent.contains("Android") -> {
                val modelMatch = Regex("\\(([^;]+);([^)]+)\\)").find(userAgent)
                modelMatch?.groupValues?.getOrNull(2)?.trim() ?: "Android Device"
            }
            userAgent.contains("Windows") -> "Windows PC"
            userAgent.contains("Macintosh") -> "Mac"
            userAgent.contains("Linux") -> "Linux PC"
            else -> "Unknown Device"
        }
    }

    private fun normalizeIpAddress(ip: String): String {
        return if (ip.contains('.')) {
            // For IPv4, keep first 3 octets to handle dynamic IPs in same subnet
            ip.split('.').take(3).joinToString(".")
        } else {
            // For IPv6, keep first 4 blocks
            ip.split(':').take(4).joinToString(":")
        }
    }
}