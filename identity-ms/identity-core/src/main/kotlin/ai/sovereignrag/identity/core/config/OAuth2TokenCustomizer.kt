package ai.sovereignrag.identity.core.config

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.core.config.JwtClaimName.AUTHORITIES
import ai.sovereignrag.identity.core.config.JwtClaimName.CLIENT_TYPE
import ai.sovereignrag.identity.core.config.JwtClaimName.EMAIL
import ai.sovereignrag.identity.core.config.JwtClaimName.EMAIL_VERIFIED
import ai.sovereignrag.identity.core.config.JwtClaimName.ENVIRONMENT
import ai.sovereignrag.identity.core.config.JwtClaimName.ENVIRONMENT_CONFIG
import ai.sovereignrag.identity.core.config.JwtClaimName.ENVIRONMENT_PREFERENCE
import ai.sovereignrag.identity.core.config.JwtClaimName.FIRST_NAME
import ai.sovereignrag.identity.core.config.JwtClaimName.LAST_NAME
import ai.sovereignrag.identity.core.config.JwtClaimName.MERCHANT_ENVIRONMENT_MODE
import ai.sovereignrag.identity.core.config.JwtClaimName.MERCHANT_ID
import ai.sovereignrag.identity.core.config.JwtClaimName.ORGANIZATION_ID
import ai.sovereignrag.identity.core.config.JwtClaimName.NAME
import ai.sovereignrag.identity.core.config.JwtClaimName.ORGANIZATION_STATUS
import ai.sovereignrag.identity.core.config.JwtClaimName.PHONE_NUMBER
import ai.sovereignrag.identity.core.config.JwtClaimName.PHONE_NUMBER_VERIFIED
import ai.sovereignrag.identity.core.config.JwtClaimName.PICTURE
import ai.sovereignrag.identity.core.config.JwtClaimName.PREFERRED_USERNAME
import ai.sovereignrag.identity.core.config.JwtClaimName.REALM_ACCESS
import ai.sovereignrag.identity.core.config.JwtClaimName.RESOURCE_ACCESS
import ai.sovereignrag.identity.core.config.JwtClaimName.ROLES
import ai.sovereignrag.identity.core.config.JwtClaimName.SETUP_COMPLETED
import ai.sovereignrag.identity.core.config.JwtClaimName.TRUST_LEVEL
import ai.sovereignrag.identity.core.config.JwtClaimName.TYPE
import ai.sovereignrag.identity.core.config.JwtClaimName.VERIFICATION_STATUS
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

private val log = KotlinLogging.logger {}

@Configuration
class OAuth2TokenCustomizerConfig {

    companion object {
        private const val PRESIGNED_URL_EXPIRATION_MINUTES = 60L
    }

    @Bean
    fun jwtTokenCustomizer(
        clientRepository: OAuthRegisteredClientRepository,
        userRepository: OAuthUserRepository,
        fileUploadGateway: FileUploadGateway
    ): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            log.info { "Customizing token for grant type: ${context.authorizationGrantType?.value}" }

            normalizeScopes(context)

            when {
                context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    customizeClientCredentialsToken(context, clientRepository)
                }

                context.tokenType == OAuth2TokenType.ACCESS_TOKEN -> {
                    customizeUserAccessToken(context, userRepository, clientRepository, fileUploadGateway)
                }
            }
        }
    }

    private fun normalizeScopes(context: JwtEncodingContext) {
        val scopes = context.authorizedScopes
        scopes.takeIf { it.isNotEmpty() }?.let {
            context.claims.claim("scope", it.joinToString(" "))
        }
    }

    private fun customizeClientCredentialsToken(
        context: JwtEncodingContext,
        clientRepository: OAuthRegisteredClientRepository
    ) {
        val clientId = context.registeredClient?.clientId
        log.info { "Customizing client credentials token for client: $clientId" }

        runCatching {
            val client = clientId?.let { clientRepository.findByClientId(it) }

            if (client?.isKnowledgeBaseClient() == true) {
                context.claims.claim("knowledge_base_id", client.knowledgeBaseId)
                context.claims.claim("organization_id", client.organizationId.toString())
                context.claims.claim(CLIENT_TYPE.value, "kb_api")
                context.claims.claim(TYPE.value, "KB_API")
                log.info { "Added KB claims for client: $clientId, kb: ${client.knowledgeBaseId}, org: ${client.organizationId}" }
                return@runCatching
            }

            client?.let {
                context.claims.claim(MERCHANT_ID.value, it.id)
                context.claims.claim(ENVIRONMENT.value, it.environmentMode.name)
                log.info { "Added merchant_id: ${it.id} and environment: ${it.environmentMode} for client: $clientId" }
            } ?: context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)

            context.claims.claim(TYPE.value, "BUSINESS")
            context.claims.claim(CLIENT_TYPE.value, "service")
        }.onFailure { e ->
            log.error(e) { "Failed to fetch client data for: $clientId" }
            context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
        }
    }

    private fun customizeUserAccessToken(
        context: JwtEncodingContext,
        userRepository: OAuthUserRepository,
        clientRepository: OAuthRegisteredClientRepository,
        fileUploadGateway: FileUploadGateway
    ) {
        val principal = context.getPrincipal<org.springframework.security.core.Authentication>()
        val username = principal?.name
            ?: return log.warn { "Username is null in token customization context" }

        log.info { "Customizing user access token for user: $username" }

        runCatching {
            val user = userRepository.findByUsername(username)
                ?: return run {
                    log.warn { "User not found: $username" }
                    context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
                }

            context.claims.subject(user.id.toString())
            log.info { "Set token subject to user ID: ${user.id}" }

            val merchant = user.merchantId?.toString()?.let { clientRepository.findByIdWithSettings(it).orElse(null) }
            val userEnvironmentPreference = user.environmentPreference
            val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX
            val effectiveEnvironment = merchantEnvironmentMode
                .takeIf { it == EnvironmentMode.PRODUCTION }
                ?.let { userEnvironmentPreference }
                ?: merchantEnvironmentMode

            log.info {
                "Adding environment claims for user $username: " +
                        "effective=$effectiveEnvironment, " +
                        "userPref=$userEnvironmentPreference, " +
                        "merchantMode=$merchantEnvironmentMode"
            }

            val userAuthorities = principal.authorities.map { it.authority }
            val realmAccess = mapOf(ROLES.value to (listOf("offline_access", "uma_authorization", "default-roles-akuid") + userAuthorities))
            val resourceAccess = mapOf("account" to mapOf(ROLES.value to listOf("manage-account", "manage-account-links", "view-profile")))

            val verificationStatus = mapOf(
                PHONE_NUMBER.value to user.phoneNumberVerified.toVerificationStatus(),
                EMAIL.value to user.emailVerified.toVerificationStatus()
            )

            user.merchantId?.let { context.claims.claim(MERCHANT_ID.value, it.toString()) }
            user.organizationId?.let { context.claims.claim(ORGANIZATION_ID.value, it.toString()) }

            val setupCompleted = merchant?.getSetting(OAuthClientSettingName.SETUP_COMPLETED) == "true"
            context.claims.claim(SETUP_COMPLETED.value, setupCompleted)
            merchant?.status?.name?.let { context.claims.claim(ORGANIZATION_STATUS.value, it) }

            val environmentConfig = mapOf(
                ENVIRONMENT.value to effectiveEnvironment.name,
                ENVIRONMENT_PREFERENCE.value to userEnvironmentPreference.name,
                MERCHANT_ENVIRONMENT_MODE.value to merchantEnvironmentMode.name
            )

            context.claims.claim(ENVIRONMENT_CONFIG.value, environmentConfig)
            context.claims.claim(REALM_ACCESS.value, realmAccess)
            context.claims.claim(RESOURCE_ACCESS.value, resourceAccess)
            context.claims.claim(AUTHORITIES.value, userAuthorities)
            context.claims.claim(VERIFICATION_STATUS.value, verificationStatus)
            context.claims.claim(EMAIL_VERIFIED.value, user.emailVerified)
            context.claims.claim(PHONE_NUMBER_VERIFIED.value, user.phoneNumberVerified)
            context.claims.claim(PHONE_NUMBER.value, user.phoneNumber.orEmpty())
            context.claims.claim(FIRST_NAME.value, user.firstName.orEmpty())
            context.claims.claim(LAST_NAME.value, user.lastName.orEmpty())
            context.claims.claim(EMAIL.value, user.email)
            context.claims.claim(PREFERRED_USERNAME.value, user.email)
            context.claims.claim(NAME.value, "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim().ifEmpty { user.email })
            user.pictureUrl?.let { pictureUrl ->
                val resolvedUrl = resolveAvatarUrl(pictureUrl, fileUploadGateway)
                context.claims.claim(PICTURE.value, resolvedUrl)
            }

            user.userType?.name?.let { context.claims.claim(TYPE.value, it) }
            user.trustLevel?.name?.let { context.claims.claim(TRUST_LEVEL.value, it) }
        }.onFailure { e ->
            log.error(e) { "Failed to fetch user data for: $username" }
            context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
        }
    }

    private fun Boolean.toVerificationStatus(): String = if (this) "VERIFIED" else "PENDING"

    private fun resolveAvatarUrl(storedValue: String, fileUploadGateway: FileUploadGateway): String {
        return when {
            storedValue.startsWith("http") -> storedValue
            storedValue.startsWith("avatars/") -> fileUploadGateway.generatePresignedDownloadUrl(
                storedValue,
                PRESIGNED_URL_EXPIRATION_MINUTES
            )
            else -> storedValue
        }
    }
}