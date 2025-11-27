package ai.sovereignrag.identity.core.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "oauth_scopes", schema = "identity")
class OAuthScope(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val name: String
) {
    @ManyToMany(mappedBy = "scopes")
    val clients: MutableSet<OAuthRegisteredClient> = mutableSetOf()

    constructor(name: String) : this(null, name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OAuthScope) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

@Entity
@Table(name = "oauth_authentication_methods", schema = "identity")
class OAuthAuthenticationMethod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val name: String
) {
    @ManyToMany(mappedBy = "authenticationMethods")
    val clients: MutableSet<OAuthRegisteredClient> = mutableSetOf()

    constructor(name: String) : this(null, name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OAuthAuthenticationMethod) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

@Entity
@Table(name = "oauth_grant_types", schema = "identity")
class OAuthGrantType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val name: String
) {
    @ManyToMany(mappedBy = "grantTypes")
    val clients: MutableSet<OAuthRegisteredClient> = mutableSetOf()

    constructor(name: String) : this(null, name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OAuthGrantType) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

@Entity
@Table(name = "oauth_client_redirect_uris", schema = "identity")
class OAuthClientRedirectUri(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    val uri: String
) : Serializable

@Entity
@Table(name = "oauth_client_post_logout_redirect_uris", schema = "identity")
class OAuthClientPostLogoutRedirectUri(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    val uri: String
) : Serializable

@Entity
@Table(name = "oauth_client_settings", schema = "identity")
class OAuthClientSetting(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    @Enumerated(EnumType.STRING)
    val settingName: OAuthClientSettingName,

    var settingValue: String
) : Serializable

@Entity
@Table(name = "oauth_client_token_settings", schema = "identity")
class OAuthClientTokenSetting(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    @Enumerated(EnumType.STRING)
    val settingName: OAuthTokenSettingName,

    var settingValue: String
) : Serializable
