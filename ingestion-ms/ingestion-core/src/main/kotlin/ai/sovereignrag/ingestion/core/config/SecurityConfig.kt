package ai.sovereignrag.ingestion.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    @Value("\${sovereignrag.cors.allowed-origins}") private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, JwtAuthenticationToken> {
        return IngestionJwtAuthenticationConverter()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.PATCH.name()
        )
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("Authorization")
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

class IngestionJwtAuthenticationConverter : Converter<Jwt, JwtAuthenticationToken> {

    override fun convert(jwt: Jwt): JwtAuthenticationToken {
        val tokenType = jwt.getClaim<String>("token_type")
        val typ = jwt.getClaim<String>("typ")

        if (tokenType == "refresh" || typ == "Refresh") {
            throw org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                org.springframework.security.oauth2.core.OAuth2Error(
                    "invalid_token",
                    "Refresh tokens cannot be used for authentication",
                    null
                )
            )
        }

        val authorities = extractAuthorities(jwt)
        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()

        jwt.getClaimAsStringList("authorities")?.forEach { authority ->
            authorities.add(SimpleGrantedAuthority(authority))
        }

        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        realmAccess?.let { access ->
            val roles = access["roles"] as? List<*>
            roles?.forEach { role ->
                (role as? String)?.let {
                    val authority = if (it.startsWith("ROLE_")) it else "ROLE_$it"
                    authorities.add(SimpleGrantedAuthority(authority))
                }
            }
        }

        return authorities
    }
}
