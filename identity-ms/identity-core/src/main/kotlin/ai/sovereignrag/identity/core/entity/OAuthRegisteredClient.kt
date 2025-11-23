package ai.sovereignrag.identity.core.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "oauth_registered_clients", schema = "identity")
class OAuthRegisteredClient {
    @Id
    @Column(length = 100)
    var id: String = ""

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    var clientId: String = ""

    @Column(name = "client_id_issued_at", nullable = false)
    var clientIdIssuedAt: Instant = Instant.now()

    @Column(name = "client_secret")
    var clientSecret: String? = null

    @Column(name = "client_secret_expires_at")
    var clientSecretExpiresAt: Instant? = null

    @Column(name = "sandbox_client_secret")
    var sandboxClientSecret: String? = null

    @Column(name = "sandbox_client_secret_expires_at")
    var sandboxClientSecretExpiresAt: Instant? = null

    @Column(name = "production_client_secret")
    var productionClientSecret: String? = null

    @Column(name = "production_client_secret_expires_at")
    var productionClientSecretExpiresAt: Instant? = null

    @Column(name = "client_name", nullable = false, length = 200)
    var clientName: String = ""

    @Column(name = "client_authentication_methods", nullable = false, length = 1000)
    var clientAuthenticationMethods: String = ""

    @Column(name = "authorization_grant_types", nullable = false, length = 1000)
    var authorizationGrantTypes: String = ""

    @Column(name = "redirect_uris", length = 1000)
    var redirectUris: String? = null

    @Column(name = "post_logout_redirect_uris", length = 1000)
    var postLogoutRedirectUris: String? = null

    @Column(nullable = false, length = 1000)
    var scopes: String = ""

    @Column(name = "client_settings", nullable = false, columnDefinition = "TEXT")
    var clientSettings: String = ""

    @Column(name = "token_settings", nullable = false, columnDefinition = "TEXT")
    var tokenSettings: String = ""

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(name = "failed_auth_attempts", nullable = false)
    var failedAuthAttempts: Int = 0

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null

    @Column(name = "last_failed_auth")
    var lastFailedAuth: Instant? = null

    @Column(name = "environment_mode", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var environmentMode: EnvironmentMode = EnvironmentMode.SANDBOX

    constructor() {
        // Default constructor for JPA
    }

    constructor(
        id: String,
        clientId: String,
        clientName: String,
        clientAuthenticationMethods: String,
        authorizationGrantTypes: String,
        scopes: String,
        clientSettings: String,
        tokenSettings: String
    ) {
        this.id = id
        this.clientId = clientId
        this.clientName = clientName
        this.clientAuthenticationMethods = clientAuthenticationMethods
        this.authorizationGrantTypes = authorizationGrantTypes
        this.scopes = scopes
        this.clientSettings = clientSettings
        this.tokenSettings = tokenSettings
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }

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
        
        updatedAt = now
    }

    fun resetFailedAuthAttempts() {
        failedAuthAttempts = 0
        lastFailedAuth = null
        lockedUntil = null
        updatedAt = Instant.now()
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

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
}