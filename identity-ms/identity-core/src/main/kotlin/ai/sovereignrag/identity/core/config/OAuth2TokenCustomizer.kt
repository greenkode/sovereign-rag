package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

private val log = KotlinLogging.logger {}

@Configuration
class OAuth2TokenCustomizerConfig {

    @Bean
    fun jwtTokenCustomizer(
        clientRepository: OAuthRegisteredClientRepository,
        userRepository: OAuthUserRepository
    ): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            log.info { "Customizing token for grant type: ${context.authorizationGrantType?.value}" }

            when {
                context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    customizeClientCredentialsToken(context, clientRepository)
                }

                context.tokenType == OAuth2TokenType.ACCESS_TOKEN -> {
                    customizeUserAccessToken(context, userRepository, clientRepository)
                }
            }
        }
    }

    private fun customizeClientCredentialsToken(
        context: JwtEncodingContext,
        clientRepository: OAuthRegisteredClientRepository
    ) {
        val clientId = context.registeredClient?.clientId
        log.info { "Customizing client credentials token for client: $clientId" }

        try {
            val client = clientId?.let { clientRepository.findByClientId(it) }

            if (client != null) {
                context.claims.claim("merchant_id", client.id)
                context.claims.claim("environment", client.environmentMode.name)
                log.info { "Added merchant_id: ${client.id} and environment: ${client.environmentMode} for client: $clientId" }
            } else {
                context.claims.claim("environment", EnvironmentMode.SANDBOX.name)
            }

            context.claims.claim("type", "BUSINESS")
            context.claims.claim("client_type", "service")

        } catch (e: Exception) {
            log.error(e) { "Failed to fetch client data for: $clientId" }
            context.claims.claim("environment", EnvironmentMode.SANDBOX.name)
        }
    }

    private fun customizeUserAccessToken(
        context: JwtEncodingContext,
        userRepository: OAuthUserRepository,
        clientRepository: OAuthRegisteredClientRepository
    ) {
        val principal = context.getPrincipal<org.springframework.security.core.Authentication>()
        val username = principal?.name
        log.info { "Customizing user access token for user: $username" }

        if (username == null) {
            log.warn { "Username is null in token customization context" }
            return
        }

        try {
            val user = userRepository.findByUsername(username)
            if (user == null) {
                log.warn { "User not found: $username" }
                context.claims.claim("environment", EnvironmentMode.SANDBOX.name)
                return
            }

            context.claims.subject(user.id.toString())
            log.info { "Set token subject to user ID: ${user.id}" }

            val merchant = user.merchantId?.toString()?.let { clientRepository.findById(it).orElse(null) }
            val userEnvironmentPreference = user.environmentPreference
            val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX
            val effectiveEnvironment =
                if (merchantEnvironmentMode == EnvironmentMode.PRODUCTION) userEnvironmentPreference else merchantEnvironmentMode

            log.info {
                "Adding environment claims for user $username: " +
                        "effective=$effectiveEnvironment, " +
                        "userPref=$userEnvironmentPreference, " +
                        "merchantMode=$merchantEnvironmentMode"
            }

            val userAuthorities = principal.authorities.map { it.authority }
            val realmAccess = mapOf("roles" to (listOf("offline_access", "uma_authorization", "default-roles-akuid") + userAuthorities))
            val resourceAccess = mapOf("account" to mapOf("roles" to listOf("manage-account", "manage-account-links", "view-profile")))

            val verificationStatus = mapOf(
                "phone_number" to if (user.phoneNumberVerified) "VERIFIED" else "PENDING",
                "email" to if (user.emailVerified) "VERIFIED" else "PENDING"
            )

            user.merchantId?.let {
                context.claims.claim("merchant_id", it.toString())
            }

            user.akuId?.let {
                context.claims.claim("aku_id", it.toString())
            }

            val environmentConfig = mapOf(
                "environment" to effectiveEnvironment.name,
                "environment_preference" to userEnvironmentPreference.name,
                "merchant_environment_mode" to merchantEnvironmentMode.name
            )

            context.claims.claim("environment_config", environmentConfig)

            context.claims.claim("realm_access", realmAccess)
            context.claims.claim("resource_access", resourceAccess)
            context.claims.claim("authorities", userAuthorities)
            context.claims.claim("verification_status", verificationStatus)

            context.claims.claim("email_verified", user.emailVerified)
            context.claims.claim("phone_number_verified", user.phoneNumberVerified)
            context.claims.claim("phone_number", user.phoneNumber ?: "")
            context.claims.claim("first_name", user.firstName ?: "")
            context.claims.claim("last_name", user.lastName ?: "")
            context.claims.claim("email", user.email)
            context.claims.claim("preferred_username", user.email)
            context.claims.claim("name", "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email })

            user.userType?.name?.let {
                context.claims.claim("type", it)
            }

            user.trustLevel?.name?.let {
                context.claims.claim("trust_level", it)
            }

        } catch (e: Exception) {
            log.error(e) { "Failed to fetch user data for: $username" }
            context.claims.claim("environment", EnvironmentMode.SANDBOX.name)
        }
    }
}