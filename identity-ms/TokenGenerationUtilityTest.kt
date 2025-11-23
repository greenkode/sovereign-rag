package ai.sovereignrag.identity.core.service

fun main() {
    val tokenUtility = TokenGenerationUtility()
    val userEmail = "user@example.com"
    
    println("=== Token Generation Examples ===")
    println()
    
    // 2FA numeric code
    println("2FA Code (6 digits): ${tokenUtility.generateNumericToken()}")
    println("2FA Code (8 digits): ${tokenUtility.generateNumericToken(8)}")
    println()
    
    // Session ID (hash-based, 128 chars)
    val sessionId = tokenUtility.generateSecureSessionId(userEmail)
    println("Session ID (${sessionId.length} chars): $sessionId")
    println()
    
    // Password reset/invitation tokens (100 chars)
    val resetToken = tokenUtility.generateAlphanumericToken(100)
    println("Password Reset Token (${resetToken.length} chars): $resetToken")
    println()
    
    // Client secrets (20 chars)
    val clientSecret = tokenUtility.generateAlphanumericToken(20)
    println("Client Secret (${clientSecret.length} chars): $clientSecret")
    println()
    
    // URL-safe tokens
    val urlSafeToken = tokenUtility.generateUrlSafeToken(64)
    println("URL-Safe Token (${urlSafeToken.length} chars): $urlSafeToken")
    println()
    
    // UUID for backward compatibility
    val uuidToken = tokenUtility.generateUuidToken()
    println("UUID Token: $uuidToken")
}