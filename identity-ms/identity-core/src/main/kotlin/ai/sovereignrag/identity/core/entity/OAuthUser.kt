package ai.sovereignrag.identity.core.entity

import ai.sovereignrag.identity.commons.AuditableEntity
import jakarta.persistence.*
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
    INVITATION, OAUTH_GOOGLE, OAUTH_MICROSOFT
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

    @Column(nullable = false, unique = true, length = 100)
    var username: String = ""

    @Column(nullable = false)
    var password: String = ""

    @Column(length = 255)
    var email: String = ""

    @Column(name = "first_name", length = 100)
    var firstName: String? = null

    @Column(name = "middle_name", length = 100)
    var middleName: String? = null

    @Column(name = "last_name", length = 100)
    var lastName: String? = null

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null

    @Column(name = "merchant_id")
    var merchantId: UUID? = null

    @Column(name = "aku_id")
    var akuId: UUID? = null

    @Column(name = "user_type", length = 20)
    @Enumerated(EnumType.STRING)
    var userType: UserType? = null

    @Column(name = "trust_level", length = 20)
    @Enumerated(EnumType.STRING)
    var trustLevel: TrustLevel? = null

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false

    @Column(name = "phone_number_verified", nullable = false)
    var phoneNumberVerified: Boolean = false

    @Column(name = "invitation_status", nullable = false)
    var invitationStatus: Boolean = false

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null

    @Column(name = "tax_identification_number", length = 50)
    var taxIdentificationNumber: String? = null

    @Column(name = "locale", length = 10)
    var locale: String = "en"

    @Column(nullable = false)
    var enabled: Boolean = true

    @Column(name = "account_non_expired", nullable = false)
    var accountNonExpired: Boolean = true

    @Column(name = "account_non_locked", nullable = false)
    var accountNonLocked: Boolean = true

    @Column(name = "credentials_non_expired", nullable = false)
    var credentialsNonExpired: Boolean = true

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "oauth_user_authorities",
        schema = "identity",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "authority")
    var authorities: MutableSet<String> = mutableSetOf()

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null

    @Column(name = "last_failed_login")
    var lastFailedLogin: Instant? = null

    @Column(name = "environment_preference", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var environmentPreference: EnvironmentMode = EnvironmentMode.SANDBOX

    @Column(name = "environment_last_switched_at")
    var environmentLastSwitchedAt: Instant? = null

    @Column(name = "registration_source", length = 50)
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

    /**
     * Checks if the account is currently locked due to failed login attempts
     */
    fun isCurrentlyLocked(): Boolean {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil)
    }

    /**
     * Records a failed login attempt and locks the account if threshold is reached
     */
    fun recordFailedLogin() {
        val now = Instant.now()
        failedLoginAttempts++
        lastFailedLogin = now
        
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60)
            accountNonLocked = false
        }
    }

    /**
     * Resets failed login attempts and unlocks the account
     */
    fun resetFailedLoginAttempts() {
        failedLoginAttempts = 0
        lastFailedLogin = null
        lockedUntil = null
        accountNonLocked = true
    }

    /**
     * Checks if the lockout period has expired and unlocks the account if so
     */
    fun checkAndUnlockIfExpired(): Boolean {
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            resetFailedLoginAttempts()
            return true
        }
        return false
    }

    /**
     * Returns display name combining first and last names, falling back to username
     */
    fun fullName(): String {
        val parts = listOfNotNull(firstName, lastName)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else username
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
}