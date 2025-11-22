package ai.sovereignrag.auth

import ai.sovereignrag.auth.authentication.TenantAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
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
    @Lazy private val tenantAuthenticationProvider: TenantAuthenticationProvider
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
            // Disable CSRF (not needed for stateless JWT authentication)
            .csrf { it.disable() }

            // Enable CORS with configuration
            .cors { it.configurationSource(corsConfigurationSource()) }

            // Configure authorization rules
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints (no authentication required)
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info", "/api/admin/tenants/{tenantId}/request-reset",
                        "/api/admin/tenants/{tenantId}/confirm-reset").permitAll()
                    .anyRequest().authenticated()
            }

            // Stateless sessions (JWT doesn't need sessions)
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Add JWT filter before Spring Security's authentication filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    /**
     * CORS Configuration
     *
     * Allows WordPress sites to make requests to the API from browser
     * IMPORTANT: Configure allowedOriginPatterns properly for production
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // PRODUCTION: Replace with specific WordPress domains
        // Example: listOf("https://example.com", "https://www.example.com")
        configuration.allowedOriginPatterns = listOf("*")

        // Allow common HTTP methods
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        )

        // Allow all headers (can be restricted in production)
        configuration.allowedHeaders = listOf("*")

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Expose Authorization header to JavaScript
        configuration.exposedHeaders = listOf("Authorization")

        // Cache preflight responses for 1 hour
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
