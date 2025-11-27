package ai.sovereignrag.identity.core.entity

import ai.sovereignrag.identity.commons.AuditableEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class UserType {
    INDIVIDUAL, BUSINESS
}

enum class TrustLevel {
    TIER_ONE, TIER_TWO, TIER_THREE, TIER_ZERO
}

enum class RegistrationSource {
    INVITATION, OAUTH_GOOGLE, OAUTH_MICROSOFT, SELF_REGISTRATION
}

enum class OAuthProvider {
    GOOGLE, MICROSOFT
}

@Entity
@Table(name = "oauth_users", schema = "identity")
class OAuthUser() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var username: String = ""

    var password: String = ""

    var email: String = ""

    var firstName: String? = null

    var middleName: String? = null

    var lastName: String? = null

    var phoneNumber: String? = null

    var merchantId: UUID? = null

    @Enumerated(EnumType.STRING)
    var userType: UserType? = null

    @Enumerated(EnumType.STRING)
    var trustLevel: TrustLevel? = null

    var emailVerified: Boolean = false

    var phoneNumberVerified: Boolean = false

    var invitationStatus: Boolean = false

    var dateOfBirth: LocalDate? = null

    var taxIdentificationNumber: String? = null

    var locale: String = "en"

    var enabled: Boolean = true

    var accountNonExpired: Boolean = true

    var accountNonLocked: Boolean = true

    var credentialsNonExpired: Boolean = true

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "oauth_user_authorities",
        schema = "identity",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "authority")
    var authorities: MutableSet<String> = mutableSetOf()

    var failedLoginAttempts: Int = 0

    var lockedUntil: Instant? = null

    var lastFailedLogin: Instant? = null

    @Enumerated(EnumType.STRING)
    var environmentPreference: EnvironmentMode = EnvironmentMode.SANDBOX

    var environmentLastSwitchedAt: Instant? = null

    @Enumerated(EnumType.STRING)
    var registrationSource: RegistrationSource = RegistrationSource.INVITATION

    constructor(username: String, password: String) : this() {
        this.username = username
        this.password = password
    }

    constructor(
        username: String,
        password: String,
        email: String,
        enabled: Boolean = true,
        authorities: MutableSet<String> = mutableSetOf()
    ) : this() {
        this.username = username
        this.password = password
        this.email = email
        this.enabled = enabled
        this.authorities = authorities
    }

    fun isCurrentlyLocked(): Boolean {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil)
    }

    fun recordFailedLogin() {
        val now = Instant.now()
        failedLoginAttempts++
        lastFailedLogin = now

        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60)
            accountNonLocked = false
        }
    }

    fun resetFailedLoginAttempts() {
        failedLoginAttempts = 0
        lastFailedLogin = null
        lockedUntil = null
        accountNonLocked = true
    }

    fun checkAndUnlockIfExpired(): Boolean {
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            resetFailedLoginAttempts()
            return true
        }
        return false
    }

    fun fullName(): String {
        val parts = listOfNotNull(firstName, lastName)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else username
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
}
