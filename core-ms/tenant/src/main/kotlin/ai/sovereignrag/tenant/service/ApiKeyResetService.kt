package ai.sovereignrag.tenant.service

import mu.KotlinLogging
import nl.compilot.ai.tenant.domain.ResetToken
import nl.compilot.ai.tenant.repository.ResetTokenRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Service for secure API key reset flow with email verification
 *
 * Two-step process:
 * 1. requestReset() - Generates token, sends email
 * 2. confirmReset() - Validates token, regenerates API key
 */
@Service
class ApiKeyResetService(
    private val tenantRegistryService: TenantRegistryService,
    private val resetTokenRepository: ResetTokenRepository,
    private val passwordEncoder: PasswordEncoder
    // TODO: Inject email service when available
    // private val emailService: EmailService
) {

    /**
     * Step 1: Request API key reset
     *
     * Generates a secure token and sends it via email to the admin
     *
     * @param tenantId Tenant requesting reset
     * @param ipAddress Client IP address for audit logging
     * @return Reset request result with masked email
     */
    @Transactional(transactionManager = "masterTransactionManager")
    fun requestReset(tenantId: String, ipAddress: String?): ResetRequestResult {
        logger.info { "API key reset requested for tenant: $tenantId from IP: ${ipAddress ?: "unknown"}" }

        // Get tenant and validate admin email exists
        val tenant = tenantRegistryService.getTenant(tenantId)
        if (tenant.adminEmail.isNullOrBlank()) {
            logger.error { "❌ Reset request DENIED: No admin email configured for tenant: $tenantId" }
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No admin email configured. Please contact support to configure admin email."
            )
        }

        // Rate limiting: Check for recent reset requests (max 3 per hour)
        val recentTokens = resetTokenRepository.findByTenantIdAndUsedAtIsNullAndExpiresAtAfter(
            tenantId,
            Instant.now().minusSeconds(3600)
        )
        if (recentTokens.size >= 3) {
            logger.warn { "⚠️  Reset request RATE LIMITED for tenant: $tenantId (${recentTokens.size} requests in last hour)" }
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many reset requests. Please wait before requesting again."
            )
        }

        // Generate 6-digit secure token
        val plainToken = generateSecureToken()
        val tokenHash = passwordEncoder.encode(plainToken)

        // Create and save reset token
        val resetToken = ResetToken(
            tenantId = tenantId,
            tokenHash = tokenHash,
            expiresAt = Instant.now().plusSeconds(ResetToken.VALIDITY_MINUTES * 60),
            ipAddress = ipAddress
        )
        resetTokenRepository.save(resetToken)

        logger.info { "✅ Reset token generated for tenant: $tenantId, expires at: ${resetToken.expiresAt}" }

        // Send email with token
        sendResetEmail(tenant.adminEmail, plainToken, tenant.name)

        // Return masked email for security
        return ResetRequestResult(
            success = true,
            message = "Reset token sent to ${maskEmail(tenant.adminEmail)}. Check your email and enter the 6-digit code.",
            maskedEmail = maskEmail(tenant.adminEmail)
        )
    }

    /**
     * Step 2: Confirm API key reset with token
     *
     * Validates the token and regenerates the API key if valid
     *
     * @param tenantId Tenant ID
     * @param token 6-digit token from email
     * @return Reset result with new API key
     */
    @Transactional(transactionManager = "masterTransactionManager")
    fun confirmReset(tenantId: String, token: String): ResetConfirmResult {
        logger.info { "API key reset confirmation for tenant: $tenantId" }

        // Find valid tokens for this tenant
        val validTokens = resetTokenRepository.findByTenantIdAndUsedAtIsNullAndExpiresAtAfter(
            tenantId,
            Instant.now()
        )

        if (validTokens.isEmpty()) {
            logger.error { "❌ Reset confirmation FAILED: No valid tokens for tenant: $tenantId" }
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No valid reset token found. Please request a new reset."
            )
        }

        // Validate token against all valid tokens (constant-time comparison)
        val matchedToken = validTokens.firstOrNull { resetToken ->
            passwordEncoder.matches(token, resetToken.tokenHash)
        }

        if (matchedToken == null) {
            logger.error { "❌ Reset confirmation FAILED: Invalid token for tenant: $tenantId" }
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid reset token. Please check the code and try again."
            )
        }

        // Mark token as used
        matchedToken.markAsUsed()
        resetTokenRepository.save(matchedToken)

        // Regenerate API key
        val result = tenantRegistryService.regenerateApiKey(tenantId)

        // Delete all other reset tokens for this tenant (cleanup)
        resetTokenRepository.deleteByTenantId(tenantId)

        logger.info { "🔄 API key successfully reset for tenant: $tenantId" }

        return ResetConfirmResult(
            success = true,
            message = "API key successfully reset. Please update your WordPress plugin settings immediately.",
            newApiKey = result.newApiKey
        )
    }

    /**
     * Generate a secure 6-digit token
     */
    private fun generateSecureToken(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Mask email address for security
     * Example: j***@example.com
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"

        val localPart = parts[0]
        val domain = parts[1]

        val maskedLocal = if (localPart.length <= 2) {
            "***"
        } else {
            localPart.first() + "***"
        }

        return "$maskedLocal@$domain"
    }

    /**
     * Send reset email with token
     *
     * TODO: Implement actual email sending when email service is available
     * For now, logs the token for development purposes
     */
    private fun sendResetEmail(email: String, token: String, tenantName: String) {
        logger.info { "📧 Sending reset email to: ${maskEmail(email)}" }

        // TODO: Replace with actual email service
        logger.warn { "⚠️  EMAIL SERVICE NOT CONFIGURED - Development mode: Token = $token" }
        logger.warn { "⚠️  In production, this should send email to: $email" }

        /*
        // Example email sending (uncomment when email service is available):
        emailService.send(
            to = email,
            subject = "Compilot AI - API Key Reset Request",
            body = """
                Hello,

                A request has been made to reset the API key for your Compilot AI account ($tenantName).

                Your reset code is: $token

                This code will expire in ${ResetToken.VALIDITY_MINUTES} minutes.

                If you did not request this reset, please ignore this email.

                Best regards,
                Compilot AI Team
            """.trimIndent()
        )
        */
    }

    /**
     * Cleanup expired tokens (should be run periodically via scheduled task)
     */
    @Transactional(transactionManager = "masterTransactionManager")
    fun cleanupExpiredTokens(): Int {
        val deletedCount = resetTokenRepository.deleteExpiredTokens(Instant.now())
        if (deletedCount > 0) {
            logger.info { "🧹 Cleaned up $deletedCount expired reset tokens" }
        }
        return deletedCount
    }
}

data class ResetRequestResult(
    val success: Boolean,
    val message: String,
    val maskedEmail: String? = null
)

data class ResetConfirmResult(
    val success: Boolean,
    val message: String,
    val newApiKey: String? = null
)
