package ai.sovereignrag.identity.core.entity

import ai.sovereignrag.identity.commons.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "oauth_provider_accounts", schema = "identity")
class OAuthProviderAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: OAuthUser,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val provider: OAuthProvider,

    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String,

    @Column(name = "provider_email")
    val providerEmail: String? = null,

    @Column(name = "linked_at", nullable = false)
    val linkedAt: Instant = Instant.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null
) : AuditableEntity() {

    fun updateLastLogin() {
        this.lastLoginAt = Instant.now()
    }
}
