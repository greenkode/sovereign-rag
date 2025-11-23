package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val oauthUser: OAuthUser
) : UserDetails {
    
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return oauthUser.authorities.map { SimpleGrantedAuthority(it) }
    }
    
    override fun getPassword(): String = oauthUser.password
    
    override fun getUsername(): String = oauthUser.username
    
    override fun isAccountNonExpired(): Boolean = oauthUser.accountNonExpired
    
    override fun isAccountNonLocked(): Boolean = oauthUser.accountNonLocked
    
    override fun isCredentialsNonExpired(): Boolean = oauthUser.credentialsNonExpired
    
    override fun isEnabled(): Boolean = oauthUser.enabled
    
    // Additional properties for JWT token
    fun getOAuthUser(): OAuthUser = oauthUser
    
    fun getFullName(): String {
        val parts = listOfNotNull(oauthUser.firstName, oauthUser.lastName)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else oauthUser.username
    }
}