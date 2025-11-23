package ai.sovereignrag.identity.core.service

import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TokenGenerationUtility {

    private val secureRandom = SecureRandom()

    /**
     * Generate numeric token for 2FA codes
     * Default length: 6 digits
     */
    fun generateNumericToken(length: Int = 6): String {
        return (1..length)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
    }
}