package ai.sovereignrag.identity.core.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class ClientIpExtractionService {

    /**
     * Extract the real client IP address from HTTP request
     * Prefers IPv4 over IPv6 addresses
     */
    fun getClientIpAddress(request: HttpServletRequest): String {
        val candidateIps = mutableListOf<String>()

        // Check X-Forwarded-For header first (most common proxy header)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2...)
            xForwardedFor.split(",").forEach { ip ->
                val trimmedIp = ip.trim()
                if (trimmedIp.isNotBlank()) {
                    candidateIps.add(trimmedIp)
                }
            }
        }

        // Check X-Real-IP header (Nginx proxy header)
        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            candidateIps.add(xRealIp.trim())
        }

        // Add remote address as fallback
        candidateIps.add(request.remoteAddr)

        // Prioritize public IPv4 addresses over private IPs and IPv6
        return candidateIps.firstOrNull { isValidIPv4(it) && !isPrivateIp(it) }
            ?: candidateIps.firstOrNull { isValidIPv4(it) }
            ?: candidateIps.firstOrNull()
            ?: "unknown"
    }

    /**
     * Get client IP from current request context (Spring RequestContextHolder)
     * This method matches the approach used in core-ms RequestContextService
     */
    fun getClientIpAddressFromContext(): String {
        return try {
            val request = getCurrentHttpRequest()
            request?.let { getClientIpAddress(it) } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get comprehensive client information for logging/debugging
     */
    fun getClientInfo(request: HttpServletRequest): ClientInfo {
        val ip = getClientIpAddress(request)
        val userAgent = request.getHeader("User-Agent") ?: "unknown"
        val referer = request.getHeader("Referer")
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        val remoteAddr = request.remoteAddr

        return ClientInfo(
            ipAddress = ip,
            userAgent = userAgent,
            referer = referer,
            xForwardedFor = xForwardedFor,
            xRealIp = xRealIp,
            remoteAddr = remoteAddr
        )
    }

    private fun getCurrentHttpRequest(): HttpServletRequest? {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            requestAttributes.request
        } else {
            null
        }
    }

    private fun isValidIPv4(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false

        return parts.all { part ->
            try {
                val num = part.toInt()
                num in 0..255
            } catch (e: NumberFormatException) {
                false
            }
        }
    }

    private fun isPrivateIp(ip: String): Boolean {
        if (!isValidIPv4(ip)) return false

        val parts = ip.split(".").map { it.toInt() }

        return when {
            parts[0] == 10 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            parts[0] == 127 -> true
            parts[0] == 169 && parts[1] == 254 -> true
            else -> false
        }
    }
}

data class ClientInfo(
    val ipAddress: String,
    val userAgent: String,
    val referer: String?,
    val xForwardedFor: String?,
    val xRealIp: String?,
    val remoteAddr: String?
)