package ai.sovereignrag.identity.core.auth.api

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.dto.ClientSettings
import ai.sovereignrag.identity.commons.dto.EndpointsInfo
import ai.sovereignrag.identity.commons.dto.ErrorResponse
import ai.sovereignrag.identity.commons.dto.HomeResponse
import ai.sovereignrag.identity.commons.dto.MerchantInfoResponse
import ai.sovereignrag.identity.commons.dto.TokenResponse
import ai.sovereignrag.identity.commons.dto.UserInfoResponse
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.ClientIpExtractionService
import ai.sovereignrag.identity.core.service.CustomUserDetails
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Schema(description = "Login request with username and password")
data class LoginRequest(
    @Schema(description = "Username or email address", example = "user@example.com", required = true)
    val username: String,
    @Schema(description = "User password", example = "password123", required = true)
    val password: String,
    @Schema(description = "Optional redirect URL after successful login", example = "https://app.example.com/dashboard", required = false)
    val redirectUrl: String? = null
)

@RestController
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
class AuthController(
    private val jwtEncoder: JwtEncoder,
    private val authenticationManager: AuthenticationManager,
    private val userRepository: OAuthUserRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val objectMapper: ObjectMapper,
    private val accountLockoutService: AccountLockoutService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val clientIpExtractionService: ClientIpExtractionService,
    @Value("\${identity-ms.base-url}") private val identityBaseUrl: String,
    @Value("\${identity-ms.token.expiry:600}") private val tokenExpiry: Long,
) {

    companion object {
        private const val DEFAULT_RESOURCE_CLIENT_ID = "akupay-payment-gateway"
        private val DEFAULT_REALM_ROLES = listOf("offline_access", "uma_authorization", "default-roles-akuid")
        private val DEFAULT_ACCOUNT_ROLES = listOf("manage-account", "manage-account-links", "view-profile")
        private const val DEFAULT_SCOPE = "openid email phone profile"
        private const val DEFAULT_LOW_BALANCE_ALERT = 50000
        private const val DEFAULT_FAILURE_RATE = 5
    }

    private fun parseClientSettings(clientSettings: String?, clientId: String): ClientSettings {
        return clientSettings?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { objectMapper.readValue(it, ClientSettings::class.java) }
                    .onFailure { e -> log.warn(e) { "Failed to parse client_settings JSON for clientId: $clientId" } }
                    .getOrNull()
            }
            ?: ClientSettings.fallback(clientId)
    }

    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "Home endpoint", description = "Returns service information and available endpoints")
    @ApiResponse(responseCode = "200", description = "Service information",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = HomeResponse::class))])
    fun home(principal: Principal?): HomeResponse {
        log.info { "Home endpoint accessed by: ${principal?.name}" }
        return HomeResponse(
            message = "Identity Service Home",
            user = principal?.name ?: "anonymous",
            authenticated = principal != null,
            endpoints = EndpointsInfo()
        )
    }

    @GetMapping("/login-success")
    @ResponseBody
    @Operation(summary = "OAuth login success callback", description = "Handles successful OAuth2 authentication")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token generated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TokenResponse::class))]),
        ApiResponse(responseCode = "302", description = "Redirect to frontend with token")
    ])
    @SecurityRequirement(name = "OAuth2")
    fun loginSuccess(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @Parameter(description = "Optional redirect URL") @RequestParam(required = false) redirect: String?
    ): ResponseEntity<Any> {
        log.info { "Login success for user: ${userDetails?.username}" }

        val customUserDetails = userDetails as? CustomUserDetails
        val oauthUser = customUserDetails?.getOAuthUser()

        val token = generateOAuthToken(customUserDetails, oauthUser)

        oauthUser?.let { publishOAuthLoginAudit(it, userDetails, redirect) }

        return redirect?.takeIf { it.isNotBlank() }
            ?.let {
                val redirectUrl = "$it?token=$token&expires_in=$tokenExpiry"
                log.info { "Redirecting to frontend: $redirectUrl" }
                ResponseEntity.status(302).header("Location", redirectUrl).build()
            }
            ?: ResponseEntity.ok(TokenResponse(
                accessToken = token,
                expiresIn = tokenExpiry,
                scope = DEFAULT_SCOPE
            ))
    }

    private fun generateOAuthToken(customUserDetails: CustomUserDetails?, oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser?): String {
        val now = Instant.now()
        val authTime = now.minusSeconds(1)
        val expiry = now.plusSeconds(tokenExpiry)
        val jti = UUID.randomUUID().toString()
        val sid = UUID.randomUUID().toString()

        val userAuthorities = customUserDetails?.authorities?.map { it.authority } ?: emptyList()

        val claims = JwtClaimsSet.builder()
            .issuer(identityBaseUrl)
            .subject(oauthUser?.id?.toString() ?: UUID.randomUUID().toString())
            .audience(listOf("account"))
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("auth_time", authTime.epochSecond)
            .claim("jti", jti)
            .claim("typ", "Bearer")
            .claim("azp", DEFAULT_RESOURCE_CLIENT_ID)
            .claim("sid", sid)
            .claim("acr", "1")
            .claim("allowed-origins", listOf("https://oauth.pstmn.io"))
            .claim("realm_access", mapOf("roles" to (DEFAULT_REALM_ROLES + userAuthorities)))
            .claim("resource_access", mapOf("account" to mapOf("roles" to DEFAULT_ACCOUNT_ROLES)))
            .claim("authorities", userAuthorities)
            .claim("scope", DEFAULT_SCOPE)
            .claim("email_verified", oauthUser?.emailVerified ?: false)
            .claim("name", customUserDetails?.getFullName() ?: "Unknown User")
            .claim("last_name", oauthUser?.lastName ?: "")
            .claim("phone_number_verified", oauthUser?.phoneNumberVerified ?: false)
            .claim("phone_number", oauthUser?.phoneNumber ?: "")
            .claim("preferred_username", oauthUser?.email ?: oauthUser?.username ?: "")
            .claim("merchant_id", oauthUser?.merchantId?.toString() ?: "")
            .claim("type", oauthUser?.userType?.name ?: "INDIVIDUAL")
            .claim("verification_status", mapOf(
                "phone_number" to if (oauthUser?.phoneNumberVerified == true) "VERIFIED" else "PENDING",
                "email" to if (oauthUser?.emailVerified == true) "VERIFIED" else "PENDING"
            ))
            .claim("first_name", oauthUser?.firstName ?: "")
            .claim("email", oauthUser?.email ?: "")
            .claim("aku_id", oauthUser?.akuId?.toString() ?: "")
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }

    private fun publishOAuthLoginAudit(
        oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser,
        userDetails: UserDetails?,
        redirect: String?
    ) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = oauthUser.id.toString(),
                actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim().ifEmpty { oauthUser.email },
                merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login successful via OAuth - Username: ${userDetails?.username}",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "username" to (userDetails?.username ?: "unknown"),
                    "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                    "userId" to oauthUser.id.toString(),
                    "loginMethod" to "oauth_login",
                    "hasRedirect" to (!redirect.isNullOrBlank()).toString()
                )
            )
        )
    }

    @GetMapping("/api/userinfo")
    @ResponseBody
    @Operation(summary = "Get user information", description = "Retrieve user details by AKU ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User information retrieved",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = UserInfoResponse::class))]),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun apiUserInfo(
        @Parameter(description = "AKU ID of the user", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam(required = false) akuId: String?,
        @AuthenticationPrincipal jwt: Jwt?
    ): Any {
        log.info { "API UserInfo requested for akuId: $akuId, client: ${jwt?.subject}" }

        val scopes = extractScopes(jwt)
        if (!scopes.contains("profile") && !scopes.contains("read")) {
            log.warn { "Insufficient scope for client: ${jwt?.subject}, scopes: $scopes" }
            return ErrorResponse(
                error = "insufficient_scope",
                message = "Client requires 'profile' or 'read' scope to access user information"
            )
        }

        return akuId?.let { findUserByAkuId(it, jwt?.subject) }
            ?: buildClientInfoResponse(jwt, scopes)
    }

    private fun extractScopes(jwt: Jwt?): List<String> {
        return jwt?.getClaimAsStringList("scope")
            ?: jwt?.getClaimAsString("scope")?.split(" ")
            ?: emptyList()
    }

    private fun findUserByAkuId(akuId: String, requestedBy: String?): Any {
        return runCatching { UUID.fromString(akuId) }
            .mapCatching { uuid ->
                userRepository.findByAkuId(uuid)?.let { user ->
                    UserInfoResponse(
                        sub = user.id.toString(),
                        akuId = user.akuId.toString(),
                        name = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim(),
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        phone = user.phoneNumber,
                        username = user.username,
                        userType = user.userType?.name ?: "INDIVIDUAL",
                        trustLevel = user.trustLevel?.name ?: "UNVERIFIED",
                        emailVerified = user.emailVerified,
                        phoneVerified = user.phoneNumberVerified,
                        merchantId = user.merchantId?.toString(),
                        source = "identity-database",
                        requestedBy = requestedBy
                    )
                } ?: run {
                    log.warn { "User not found for akuId: $akuId" }
                    ErrorResponse(error = "user_not_found", message = "No user found with akuId: $akuId")
                }
            }
            .getOrElse {
                log.error { "Invalid UUID format for akuId: $akuId" }
                ErrorResponse(error = "invalid_aku_id", message = "Invalid akuId format: $akuId")
            }
    }

    private fun buildClientInfoResponse(jwt: Jwt?, scopes: List<String>): UserInfoResponse {
        return UserInfoResponse(
            sub = jwt?.subject ?: "unknown-client",
            name = "Service Client",
            clientId = jwt?.getClaimAsString("azp"),
            scopes = scopes,
            authenticated = true,
            source = "identity-service"
        )
    }

    @GetMapping("/api/merchantinfo")
    @ResponseBody
    @Operation(summary = "Get merchant information", description = "Retrieve merchant details by merchant ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Merchant information retrieved",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = MerchantInfoResponse::class))]),
        ApiResponse(responseCode = "404", description = "Merchant not found")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun apiMerchantInfo(
        @Parameter(description = "Merchant ID", example = "merchant-123")
        @RequestParam(required = false) merchantId: String?,
        @AuthenticationPrincipal jwt: Jwt?
    ): Any {
        log.info { "API MerchantInfo requested for merchantId: $merchantId, client: ${jwt?.subject}" }

        val scopes = extractScopes(jwt)
        if (!scopes.contains("profile") && !scopes.contains("read")) {
            log.warn { "Insufficient scope for client: ${jwt?.subject}, scopes: $scopes" }
            return ErrorResponse(
                error = "insufficient_scope",
                message = "Client requires 'profile' or 'read' scope to access merchant information"
            )
        }

        val targetMerchantId = merchantId ?: jwt?.getClaimAsString("merchant_id")

        return targetMerchantId
            ?.let { findMerchantById(it, jwt?.subject, merchantId == null) }
            ?: ErrorResponse(error = "no_merchant_context", message = "No merchant context found")
    }

    private fun findMerchantById(merchantId: String, requestedBy: String?, isAuthenticated: Boolean): Any {
        return oAuthRegisteredClientRepository.findById(merchantId)
            .map<Any> { merchant ->
                val settings = parseClientSettings(merchant.clientSettings, merchant.clientId)
                MerchantInfoResponse(
                    merchantId = merchant.id,
                    name = merchant.clientName,
                    email = settings.emailAddress,
                    phone = settings.phoneNumber,
                    clientId = merchant.clientId,
                    requestedBy = requestedBy,
                    authenticatedMerchant = if (isAuthenticated) true else null,
                    lowBalanceAlert = settings.lowBalance ?: DEFAULT_LOW_BALANCE_ALERT,
                    failureRate = settings.failureRate ?: DEFAULT_FAILURE_RATE
                )
            }
            .orElseGet {
                log.warn { "Merchant not found for merchantId: $merchantId" }
                ErrorResponse(error = "merchant_not_found", message = "No merchant found with merchantId: $merchantId")
            }
    }

    @GetMapping("/userinfo")
    @ResponseBody
    @Operation(summary = "Get authenticated user info", description = "Returns information about the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "User information",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = UserInfoResponse::class))])
    @SecurityRequirement(name = "OAuth2")
    fun userInfo(@AuthenticationPrincipal userDetails: UserDetails?): UserInfoResponse {
        log.info { "UserInfo requested for: ${userDetails?.username}" }
        return UserInfoResponse(
            sub = userDetails?.username ?: "unknown",
            name = userDetails?.username ?: "unknown",
            email = "${userDetails?.username}@example.com",
            authorities = userDetails?.authorities?.map { it.authority }
        )
    }
}
