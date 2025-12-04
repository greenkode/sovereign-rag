package ai.sovereignrag.identity.core.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

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
        log.debug { "JWT subject: ${jwt.subject}, extracted authorities: $authorities" }
        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()

        jwt.getClaimAsString("scope")?.split(" ")?.forEach { scope ->
            authorities.add(SimpleGrantedAuthority("SCOPE_$scope"))
        }

        jwt.getClaimAsStringList("authorities")?.forEach { authority ->
            authorities.add(SimpleGrantedAuthority(authority))
        }

        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        realmAccess?.let {
            (it["roles"] as? List<*>)?.filterIsInstance<String>()?.forEach { role ->
                val authority = role.takeIf { r -> r.startsWith("ROLE_") } ?: "ROLE_$role"
                authorities.add(SimpleGrantedAuthority(authority))
            }
        }

        return authorities
    }
}