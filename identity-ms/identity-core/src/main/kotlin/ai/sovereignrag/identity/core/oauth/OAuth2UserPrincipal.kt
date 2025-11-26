package ai.sovereignrag.identity.core.oauth

import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2UserPrincipal(
    private val oauth2User: OAuth2User,
    val internalUser: OAuthUser,
    val provider: OAuthProvider
) : OAuth2User {

    override fun getName(): String = internalUser.username

    override fun getAttributes(): MutableMap<String, Any> = oauth2User.attributes

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        internalUser.authorities.map { SimpleGrantedAuthority(it) }.toMutableList()
}
