package ai.sovereignrag.process.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class RequestContextService {

    private val log = KotlinLogging.logger {}

    fun getClientIpAddress(): String? {
        return try {
            val request = getCurrentHttpRequest()
            request?.let { extractClientIpAddress(it) }
        } catch (e: Exception) {
            log.debug(e) { "Could not extract client IP address" }
            null
        }
    }

    fun getUserAgent(): String? {
        return try {
            val request = getCurrentHttpRequest()
            request?.getHeader("User-Agent")
        } catch (e: Exception) {
            log.debug(e) { "Could not extract user agent" }
            null
        }
    }

    private fun getCurrentHttpRequest(): HttpServletRequest? {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            requestAttributes.request
        } else {
            null
        }
    }

    private fun extractClientIpAddress(request: HttpServletRequest): String {
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