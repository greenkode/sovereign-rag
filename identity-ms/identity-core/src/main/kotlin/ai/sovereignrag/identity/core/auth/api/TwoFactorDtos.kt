package ai.sovereignrag.identity.core.auth.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Two-factor authentication login request")
data class TwoFactorLoginRequest(
    @Schema(description = "Username or email address", example = "user@example.com", required = true)
    val username: String,
    @Schema(description = "User password", example = "password123", required = true)
    val password: String,
)

@Schema(description = "Two-factor authentication verification request")
data class TwoFactorVerifyRequest(
    @Schema(description = "Session ID from 2FA login", example = "sess_123456", required = true)
    val sessionId: String,
    @Schema(description = "Verification code sent to user", example = "123456", required = true)
    val code: String
)

@Schema(description = "Request to resend two-factor authentication code")
data class TwoFactorResendRequest(
    @Schema(description = "Session ID from 2FA login", example = "sess_123456", required = true)
    val sessionId: String
)

@Schema(description = "Two-factor authentication login response")
data class TwoFactorLoginResponse(
    @Schema(description = "Status of the 2FA process", example = "2FA_REQUIRED")
    val status: String,
    @Schema(description = "Session ID for 2FA verification", example = "sess_123456")
    val sessionId: String? = null,
    @Schema(description = "Response message", example = "Verification code sent to your email")
    val message: String,
    @Schema(description = "JWT access token (for direct login from trusted device)", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val accessToken: String? = null,
    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val refreshToken: String? = null,
    @Schema(description = "Token expiration time in seconds (for direct login)", example = "3600")
    val expiresIn: Long? = null
)

@Schema(description = "Two-factor authentication verification response")
data class TwoFactorVerifyResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val accessToken: String,
    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val refreshToken: String,
    @Schema(description = "Token type", example = "Bearer")
    val tokenType: String,
    @Schema(description = "Token expiration time in seconds", example = "3600")
    val expiresIn: Long,
    @Schema(description = "OAuth2 scope", example = "openid profile email")
    val scope: String,
    @Schema(description = "User information")
    val user: TwoFactorVerifyUserResponse,
    @Schema(description = "Response message", example = "Login successful")
    val message: String
)

@Schema(description = "User information in 2FA verification response")
data class TwoFactorVerifyUserResponse(
    @Schema(description = "Username", example = "john.doe")
    val username: String,
    @Schema(description = "Full name", example = "John Doe")
    val name: String,
    @Schema(description = "Email address", example = "john.doe@example.com")
    val email: String
)

@Schema(description = "Response after resending 2FA code")
data class TwoFactorResendResponse(
    @Schema(description = "Response message", example = "Verification code resent successfully")
    val message: String,
    @Schema(description = "Session ID for 2FA verification", example = "sess_123456")
    val sessionId: String
)

@Schema(description = "Refresh token request")
data class RefreshTokenRequest(
    @Schema(description = "Refresh token to exchange for new access token", example = "eyJhbGciOiJIUzI1NiIsInR...", required = true)
    val refreshToken: String
)

@Schema(description = "Refresh token response")
data class RefreshTokenResponse(
    @Schema(description = "New JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val accessToken: String,
    @Schema(description = "New refresh token", example = "eyJhbGciOiJIUzI1NiIsInR...")
    val refreshToken: String,
    @Schema(description = "Token type", example = "Bearer")
    val tokenType: String = "Bearer",
    @Schema(description = "Token expiration time in seconds", example = "3600")
    val expiresIn: Long,
    @Schema(description = "OAuth2 scope", example = "openid profile email")
    val scope: String = "openid email phone profile"
)