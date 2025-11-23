package ai.sovereignrag.identity.core.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter : Converter<Jwt, JwtAuthenticationToken> {

    override fun convert(jwt: Jwt): JwtAuthenticationToken {
        val tokenType = jwt.getClaim<String>("token_type")
        val typ = jwt.getClaim<String>("typ")

        if (tokenType == "refresh" || typ == "Refresh") {
            throw OAuth2AuthenticationException(
                OAuth2Error("invalid_token", "Refresh tokens cannot be used for authentication", null)
            )
        }

        val authorities = extractAuthorities(jwt)
        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()
        
        // Extract from direct 'authorities' claim
        jwt.getClaimAsStringList("authorities")?.forEach { authority ->
            authorities.add(SimpleGrantedAuthority(authority))
        }
        
        // Extract from 'realm_access.roles' claim
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        if (realmAccess != null) {
            val roles = realmAccess["roles"] as? List<*>
            roles?.forEach { role ->
                if (role is String) {
                    // Add roles with ROLE_ prefix if they don't already have it
                    val authority = if (role.startsWith("ROLE_")) role else "ROLE_$role"
                    authorities.add(SimpleGrantedAuthority(authority))
                }
            }
        }
        
        return authorities
    }
}