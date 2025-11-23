package ai.sovereignrag.identity.core.auth.api

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
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

    private val log = KotlinLogging.logger {}
    
    private fun extractEmailFromClientSettings(clientSettings: String?, clientId: String): String {

        return try {
            if (clientSettings.isNullOrBlank()) {
                return "merchant-$clientId@example.com" // fallback
            }
            val settingsMap = objectMapper.readValue(clientSettings, Map::class.java) as Map<String, Any>
            val emailAddress = settingsMap["emailAddress"] as? String
            emailAddress ?: "merchant-$clientId@example.com" // fallback if email_address not found
        } catch (e: Exception) {
            log.warn(e) { "Failed to parse client_settings JSON for clientId: $clientId" }
            "merchant-$clientId@example.com" // fallback on parsing error
        }
    }

    private fun extractDataFromClientSettings(clientSettings: String?, clientId: String): Map<String, String?> {
        return try {
            if (clientSettings.isNullOrBlank()) {
                return mapOf(
                    "emailAddress" to "merchant-$clientId@example.com",
                    "phoneNumber" to null
                )
            }
            val settingsMap = objectMapper.readValue(clientSettings, Map::class.java) as Map<String, Any>
            mapOf(
                "emailAddress" to (settingsMap["emailAddress"] as? String ?: "merchant-$clientId@example.com"),
                "phoneNumber" to (settingsMap["phoneNumber"] as? String)
            )
        } catch (e: Exception) {
            log.warn(e) { "Failed to parse client_settings JSON for clientId: $clientId" }
            mapOf(
                "emailAddress" to "merchant-$clientId@example.com",
                "phoneNumber" to null
            )
        }
    }
    
    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "Home endpoint", description = "Returns service information and available endpoints")
    @ApiResponse(responseCode = "200", description = "Service information",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = Map::class))])
    fun home(principal: Principal?): Map<String, Any> {
        log.info { "Home endpoint accessed by: ${principal?.name}" }
        return mapOf(
            "message" to "Identity Service Home",
            "user" to (principal?.name ?: "anonymous"),
            "authenticated" to (principal != null),
            "endpoints" to mapOf(
                "authorize" to "/oauth2/authorize",
                "token" to "/oauth2/token",
                "jwks" to "/oauth2/jwks",
                "userinfo" to "/userinfo",
                "openid-config" to "/.well-known/openid-configuration"
            )
        )
    }
    
    @GetMapping("/login-success")
    @ResponseBody
    @Operation(summary = "OAuth login success callback", description = "Handles successful OAuth2 authentication")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token generated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
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
        
        val now = Instant.now()
        val authTime = now.minusSeconds(1) // auth_time slightly before iat
        val expiry = now.plusSeconds(tokenExpiry)
        
        val jti = UUID.randomUUID().toString()
        val sid = UUID.randomUUID().toString()
        
        val verificationStatus = mapOf(
            "phone_number" to if (oauthUser?.phoneNumberVerified == true) "VERIFIED" else "PENDING",
            "email" to if (oauthUser?.emailVerified == true) "VERIFIED" else "PENDING"
        )
        
        // Include user authorities from database
        val userAuthorities = customUserDetails?.authorities?.map { it.authority } ?: emptyList()
        val realmAccess = mapOf(
            "roles" to (listOf("offline_access", "uma_authorization", "default-roles-akuid") + userAuthorities)
        )
        
        val resourceAccess = mapOf(
            "account" to mapOf(
                "roles" to listOf("manage-account", "manage-account-links", "view-profile")
            )
        )
        
        // Add authorities as a dedicated claim for easier access
        val authorities = userAuthorities
        
        val claims = JwtClaimsSet.builder()
            .issuer(identityBaseUrl)
            .subject(oauthUser?.id?.toString() ?: UUID.randomUUID().toString())
            .audience(listOf("account"))
            .issuedAt(now)
            .expiresAt(expiry)
            .claim("auth_time", authTime.epochSecond)
            .claim("jti", jti)
            .claim("typ", "Bearer")
            .claim("azp", "akupay-payment-gateway")
            .claim("sid", sid)
            .claim("acr", "1")
            .claim("allowed-origins", listOf("https://oauth.pstmn.io"))
            .claim("realm_access", realmAccess)
            .claim("resource_access", resourceAccess)
            .claim("authorities", authorities)
            .claim("scope", "openid email phone profile")
            .claim("email_verified", oauthUser?.emailVerified ?: false)
            .claim("name", customUserDetails?.getFullName() ?: "Unknown User")
            .claim("last_name", oauthUser?.lastName ?: "")
            .claim("phone_number_verified", oauthUser?.phoneNumberVerified ?: false)
            .claim("phone_number", oauthUser?.phoneNumber ?: "")
            .claim("preferred_username", oauthUser?.email ?: oauthUser?.username ?: "")
            .claim("merchant_id", oauthUser?.merchantId?.toString() ?: "")
            .claim("type", oauthUser?.userType?.name ?: "INDIVIDUAL")
            .claim("verification_status", verificationStatus)
            .claim("first_name", oauthUser?.firstName ?: "")
            .claim("email", oauthUser?.email ?: "")
            .claim("aku_id", oauthUser?.akuId?.toString() ?: "")
            .build()
        
        val token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue

        // Publish successful OAuth login audit event
        if (oauthUser != null) {
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
                    payload = mapOf<String, String>(
                        "username" to (userDetails?.username ?: "unknown"),
                        "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                        "userId" to oauthUser.id.toString(),
                        "loginMethod" to "oauth_login",
                        "hasRedirect" to (!redirect.isNullOrBlank()).toString()
                    )
                )
            )
        }

        // If redirect parameter is provided, redirect to frontend with token
        if (!redirect.isNullOrBlank()) {
            val redirectUrl = "$redirect?token=$token&expires_in=86400"
            log.info { "Redirecting to frontend: $redirectUrl" }
            return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build()
        }
        
        // Otherwise return JSON response
        val response = mapOf(
            "access_token" to token,
            "token_type" to "Bearer",
            "expires_in" to tokenExpiry,
            "scope" to "openid email phone profile"
        )
        return ResponseEntity.ok(response)
    }
    

    @GetMapping("/api/userinfo")
    @ResponseBody
    @Operation(summary = "Get user information", description = "Retrieve user details by AKU ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User information retrieved",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun apiUserInfo(
        @Parameter(description = "AKU ID of the user", example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestParam(required = false) akuId: String?,
        @AuthenticationPrincipal jwt: Jwt?
    ): Map<String, Any?> {
        log.info { "API UserInfo requested for akuId: $akuId, client: ${jwt?.subject}" }
        
        // Verify client has proper scope for userinfo access
        val scopes = jwt?.getClaimAsStringList("scope") ?: run {
            // Fallback: if scope is a string, split it
            val scopeString = jwt?.getClaimAsString("scope")
            scopeString?.split(" ") ?: emptyList()
        }
        if (!scopes.contains("profile") && !scopes.contains("read")) {
            log.warn { "Insufficient scope for client: ${jwt?.subject}, scopes: $scopes" }
            return mapOf(
                "error" to "insufficient_scope",
                "message" to "Client requires 'profile' or 'read' scope to access user information"
            )
        }
        
        return if (akuId != null) {
            try {
                val akuIdUuid = UUID.fromString(akuId)
                val user = userRepository.findByAkuId(akuIdUuid)
                
                if (user != null) {
                    mapOf(
                        "sub" to user.id.toString(),
                        "aku_id" to user.akuId.toString(),
                        "name" to "${user.firstName ?: ""} ${user.lastName ?: ""}".trim(),
                        "first_name" to (user.firstName ?: ""),
                        "last_name" to (user.lastName ?: ""),
                        "email" to user.email,
                        "phone" to (user.phoneNumber),
                        "username" to user.username,
                        "user_type" to (user.userType?.name ?: "INDIVIDUAL"),
                        "trust_level" to (user.trustLevel?.name ?: "UNVERIFIED"),
                        "email_verified" to user.emailVerified,
                        "phone_verified" to user.phoneNumberVerified,
                        "merchant_id" to user.merchantId?.toString(),
                        "source" to "identity-database",
                        "requested_by" to jwt?.subject
                    )
                } else {
                    log.warn { "User not found for akuId: $akuId" }
                    mapOf(
                        "error" to "user_not_found",
                        "message" to "No user found with akuId: $akuId"
                    )
                }
            } catch (e: IllegalArgumentException) {
                log.error { "Invalid UUID format for akuId: $akuId" }
                mapOf(
                    "error" to "invalid_aku_id",
                    "message" to "Invalid akuId format: $akuId"
                )
            }
        } else {
            // Return client info if no akuId provided
            mapOf(
                "sub" to (jwt?.subject ?: "unknown-client"),
                "name" to "Service Client",
                "client_id" to jwt?.getClaimAsString("azp"),
                "scopes" to scopes,
                "authenticated" to true,
                "source" to "identity-service"
            )
        }
    }

    @GetMapping("/api/merchantinfo")
    @ResponseBody
    @Operation(summary = "Get merchant information", description = "Retrieve merchant details by merchant ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Merchant information retrieved",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "404", description = "Merchant not found")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun apiMerchantInfo(
        @Parameter(description = "Merchant ID", example = "merchant-123")
        @RequestParam(required = false) merchantId: String?,
        @AuthenticationPrincipal jwt: Jwt?
    ): Map<String, Any?> {
        log.info { "API MerchantInfo requested for merchantId: $merchantId, client: ${jwt?.subject}" }
        
        // Verify client has proper scope for merchant info access
        val scopes = jwt?.getClaimAsStringList("scope") ?: run {
            // Fallback: if scope is a string, split it
            val scopeString = jwt?.getClaimAsString("scope")
            scopeString?.split(" ") ?: emptyList()
        }
        if (!scopes.contains("profile") && !scopes.contains("read")) {
            log.warn { "Insufficient scope for client: ${jwt?.subject}, scopes: $scopes" }
            return mapOf(
                "error" to "insufficient_scope",
                "message" to "Client requires 'profile' or 'read' scope to access merchant information"
            )
        }
        
        // Query merchant details from oauth_registered_clients table
        return if (merchantId != null) {
            val merchantOptional = oAuthRegisteredClientRepository.findById(merchantId)
            if (merchantOptional.isPresent) {
                val merchant = merchantOptional.get()
                val clientData = extractDataFromClientSettings(merchant.clientSettings, merchant.clientId)
                mapOf(
                    "merchant_id" to merchant.id,
                    "name" to merchant.clientName,
                    "email" to clientData["emailAddress"],
                    "phone" to clientData["phoneNumber"],
                    "locale" to "en_US",
                    "client_id" to merchant.clientId,
                    "source" to "identity-service",
                    "requested_by" to jwt?.subject,
                    "low_balance_alert" to (clientData["lowBalance"] ?: 50000),
                    "failureRate" to (clientData["failureRate"] ?: 5),
                )
            } else {
                log.warn { "Merchant not found for merchantId: $merchantId" }
                mapOf(
                    "error" to "merchant_not_found",
                    "message" to "No merchant found with merchantId: $merchantId"
                )
            }
        } else {
            // Return info for the authenticated client's merchant
            val clientMerchantId = jwt?.getClaimAsString("merchant_id")
            if (clientMerchantId != null) {
                val merchantOptional = oAuthRegisteredClientRepository.findById(clientMerchantId)
                if (merchantOptional.isPresent) {
                    val merchant = merchantOptional.get()
                    val clientData = extractDataFromClientSettings(merchant.clientSettings, merchant.clientId)
                    mapOf(
                        "merchant_id" to merchant.id,
                        "name" to merchant.clientName,
                        "email" to clientData["emailAddress"],
                        "phone" to clientData["phoneNumber"],
                        "locale" to "en_US",
                        "client_id" to merchant.clientId,
                        "source" to "identity-service",
                        "authenticated_merchant" to true,
                        "low_balance_alert" to (clientData["lowBalance"] ?: 50000),
                        "failureRate" to (clientData["failureRate"] ?: 5),
                    )
                } else {
                    mapOf(
                        "error" to "merchant_not_found",
                        "message" to "No merchant found with merchantId: $clientMerchantId"
                    )
                }
            } else {
                mapOf(
                    "error" to "no_merchant_context",
                    "message" to "No merchant context found"
                )
            }
        }
    }

    @GetMapping("/userinfo")
    @ResponseBody
    @Operation(summary = "Get authenticated user info", description = "Returns information about the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "User information",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = Map::class))])
    @SecurityRequirement(name = "OAuth2")
    fun userInfo(@AuthenticationPrincipal userDetails: UserDetails?): Map<String, Any> {
        log.info { "UserInfo requested for: ${userDetails?.username}" }
        return mapOf(
            "sub" to (userDetails?.username ?: "unknown"),
            "name" to (userDetails?.username ?: "unknown"),
            "email" to "${userDetails?.username}@example.com",
            "authorities" to (userDetails?.authorities?.map { it.authority } ?: emptyList())
        )
    }
}