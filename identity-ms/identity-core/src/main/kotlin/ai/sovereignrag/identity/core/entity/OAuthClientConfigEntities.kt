package ai.sovereignrag.identity.core.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Table
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import java.io.Serializable

@Entity
@Table(name = "oauth_scope")
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
@Table(name = "oauth_authentication_method")
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
@Table(name = "oauth_grant_type")
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
@Table(name = "oauth_client_redirect_uri")
class OAuthClientRedirectUri(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    val uri: String
) : Serializable

@Entity
@Table(name = "oauth_client_post_logout_redirect_uri")
class OAuthClientPostLogoutRedirectUri(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    val client: OAuthRegisteredClient,

    @Id
    val uri: String
) : Serializable

@Entity
@Table(name = "oauth_client_setting")
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
@Table(name = "oauth_client_token_setting")
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
