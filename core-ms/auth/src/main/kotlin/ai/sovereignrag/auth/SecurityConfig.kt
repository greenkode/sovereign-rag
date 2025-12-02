package ai.sovereignrag.auth

import ai.sovereignrag.auth.authentication.TenantAuthenticationProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security Configuration for JWT-based authentication
 *
 * Security Features:
 * - Custom TenantAuthenticationProvider for BCrypt-based API key validation
 * - JWT token authentication via Authorization Bearer header
 * - Stateless sessions (no server-side session storage)
 * - CORS enabled for WordPress frontend
 * - Public endpoints for authentication and health checks
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val organizationSetupFilter: OrganizationSetupFilter,
    private val knowledgeBaseContextFilter: KnowledgeBaseContextFilter,
    @Lazy private val tenantAuthenticationProvider: TenantAuthenticationProvider,
    @Value("\${sovereignrag.cors.allowed-origins}") private val allowedOrigins: String
) {

    /**
     * AuthenticationManager with custom TenantAuthenticationProvider
     *
     * This authentication manager uses BCrypt for secure API key verification
     * and provides constant-time comparison to prevent timing attacks
     */
    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(listOf(tenantAuthenticationProvider))
    }

    /**
     * DelegatingPasswordEncoder for flexible password encoding
     *
     * This encoder automatically detects the algorithm from the hash prefix:
     * - {bcrypt}$2a$12... uses BCrypt with strength 12
     *
     * BCrypt strength 12 provides a good balance between security and performance:
     * - Strength 10: ~100ms per hash (default)
     * - Strength 12: ~400ms per hash (recommended for high-security)
     * - Strength 14: ~1.6s per hash (very high security)
     *
     * No need for a separate hash_algorithm column in the database.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        val encoders = mutableMapOf<String, PasswordEncoder>(
            "bcrypt" to BCryptPasswordEncoder(12)
        )
        return DelegatingPasswordEncoder("bcrypt", encoders)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }

            .cors { it.configurationSource(corsConfigurationSource()) }

            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/auth/**",
                        "/actuator/health",
                        "/actuator/info",
                        "/api/admin/tenants/{tenantId}/request-reset",
                        "/api/admin/tenants/{tenantId}/confirm-reset"
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
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .addFilterAfter(
                organizationSetupFilter,
                JwtAuthenticationFilter::class.java
            )
            .addFilterAfter(
                knowledgeBaseContextFilter,
                OrganizationSetupFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val authorities = jwt.getClaimAsStringList("authorities") ?: emptyList()
            authorities.map { SimpleGrantedAuthority(it) }
        }
        return converter
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
