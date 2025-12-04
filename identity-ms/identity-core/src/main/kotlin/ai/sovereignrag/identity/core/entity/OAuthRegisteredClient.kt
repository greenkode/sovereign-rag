package ai.sovereignrag.identity.core.entity

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.subscription.SubscriptionTier
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Table
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import java.time.Instant
import java.util.UUID

enum class OrganizationStatus {
    ACTIVE, SUSPENDED, PENDING, DELETED
}

@Entity
@Table(name = "oauth_registered_client")
class OAuthRegisteredClient() : AuditableEntity() {
    @Id
    var id: String = ""

    var clientId: String = ""

    var clientIdIssuedAt: Instant = Instant.now()

    var clientSecret: String? = null

    var clientSecretExpiresAt: Instant? = null

    var sandboxClientSecret: String? = null

    var sandboxClientSecretExpiresAt: Instant? = null

    var productionClientSecret: String? = null

    var productionClientSecretExpiresAt: Instant? = null

    var clientName: String = ""

    var failedAuthAttempts: Int = 0

    var lockedUntil: Instant? = null

    var lastFailedAuth: Instant? = null

    @Enumerated(EnumType.STRING)
    var environmentMode: EnvironmentMode = EnvironmentMode.SANDBOX

    var domain: String? = null

    @Enumerated(EnumType.STRING)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE

    @Enumerated(EnumType.STRING)
    var plan: SubscriptionTier = SubscriptionTier.TRIAL

    var organizationId: UUID? = null

    var knowledgeBaseId: String? = null

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val redirectUris: MutableSet<OAuthClientRedirectUri> = mutableSetOf()

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val postLogoutRedirectUris: MutableSet<OAuthClientPostLogoutRedirectUri> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @JoinTable(
        name = "oauth_client_scope",
        joinColumns = [JoinColumn(name = "client_id")],
        inverseJoinColumns = [JoinColumn(name = "scope_id")]
    )
    val scopes: MutableSet<OAuthScope> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @JoinTable(
        name = "oauth_client_authentication_method",
        joinColumns = [JoinColumn(name = "client_id")],
        inverseJoinColumns = [JoinColumn(name = "method_id")]
    )
    val authenticationMethods: MutableSet<OAuthAuthenticationMethod> = mutableSetOf()

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    @JoinTable(
        name = "oauth_client_grant_type",
        joinColumns = [JoinColumn(name = "client_id")],
        inverseJoinColumns = [JoinColumn(name = "grant_type_id")]
    )
    val grantTypes: MutableSet<OAuthGrantType> = mutableSetOf()

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val settings: MutableSet<OAuthClientSetting> = mutableSetOf()

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tokenSettings: MutableSet<OAuthClientTokenSetting> = mutableSetOf()

    fun addRedirectUri(uri: String) {
        redirectUris.add(OAuthClientRedirectUri(this, uri))
    }

    fun addPostLogoutRedirectUri(uri: String) {
        postLogoutRedirectUris.add(OAuthClientPostLogoutRedirectUri(this, uri))
    }

    fun addScope(scope: OAuthScope) {
        scopes.add(scope)
        scope.clients.add(this)
    }

    fun addAuthenticationMethod(method: OAuthAuthenticationMethod) {
        authenticationMethods.add(method)
        method.clients.add(this)
    }

    fun addGrantType(grantType: OAuthGrantType) {
        grantTypes.add(grantType)
        grantType.clients.add(this)
    }

    fun addSetting(name: OAuthClientSettingName, value: String) {
        settings.removeIf { it.settingName == name }
        settings.add(OAuthClientSetting(this, name, value))
    }

    fun addTokenSetting(name: OAuthTokenSettingName, value: String) {
        tokenSettings.removeIf { it.settingName == name }
        tokenSettings.add(OAuthClientTokenSetting(this, name, value))
    }

    fun getSetting(name: OAuthClientSettingName): String? = settings.find { it.settingName == name }?.settingValue

    fun getTokenSetting(name: OAuthTokenSettingName): String? = tokenSettings.find { it.settingName == name }?.settingValue

    fun isCurrentlyLocked(): Boolean {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil)
    }

    fun recordFailedAuth() {
        val now = Instant.now()
        failedAuthAttempts++
        lastFailedAuth = now

        if (failedAuthAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60)
        }
    }

    fun resetFailedAuthAttempts() {
        failedAuthAttempts = 0
        lastFailedAuth = null
        lockedUntil = null
    }

    fun checkAndUnlockIfExpired(): Boolean {
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            resetFailedAuthAttempts()
            return true
        }
        return false
    }

    fun getClientSecretForEnvironment(environment: EnvironmentMode): String? {
        return when (environment) {
            EnvironmentMode.SANDBOX -> sandboxClientSecret ?: clientSecret
            EnvironmentMode.PRODUCTION -> productionClientSecret ?: clientSecret
        }
    }

    fun getClientSecretExpiryForEnvironment(environment: EnvironmentMode): Instant? {
        return when (environment) {
            EnvironmentMode.SANDBOX -> sandboxClientSecretExpiresAt ?: clientSecretExpiresAt
            EnvironmentMode.PRODUCTION -> productionClientSecretExpiresAt ?: clientSecretExpiresAt
        }
    }

    fun isKnowledgeBaseClient(): Boolean = knowledgeBaseId != null

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
}
