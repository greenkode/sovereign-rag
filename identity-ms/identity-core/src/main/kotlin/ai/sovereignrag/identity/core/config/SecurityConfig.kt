package ai.sovereignrag.identity.core.config

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import ai.sovereignrag.identity.core.oauth.CustomOidcUserService
import ai.sovereignrag.identity.core.oauth.OAuth2AuthenticationSuccessHandler
import ai.sovereignrag.identity.core.oauth.OAuth2AuthenticationFailureHandler

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    @Lazy private val clientLockoutAuthenticationProvider: ClientLockoutAuthenticationProvider,
    private val customOAuth2ErrorResponseHandler: CustomOAuth2ErrorResponseHandler,
    private val clientLockoutFilter: ClientLockoutFilter,
    private val jwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    @Lazy private val customOidcUserService: CustomOidcUserService,
    @Lazy private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    @Lazy private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler
) {
    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        val authorizationServerConfigurer =
            OAuth2AuthorizationServerConfigurer.authorizationServer()

        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .cors(Customizer.withDefaults())
            .addFilterBefore(clientLockoutFilter, UsernamePasswordAuthenticationFilter::class.java)
            .with(
                authorizationServerConfigurer
            ) { authorizationServer: OAuth2AuthorizationServerConfigurer? ->
                authorizationServer!!
                    .oidc(Customizer.withDefaults())
                    .clientAuthentication { clientAuth ->
                        clientAuth
                            .authenticationProviders { providers ->
                                providers.add(0, clientLockoutAuthenticationProvider)
                            }
                            .errorResponseHandler(customOAuth2ErrorResponseHandler)
                    }
            } // Enable OpenID Connect 1.0

            .authorizeHttpRequests { authorize ->
                authorize
                    .anyRequest().authenticated()
            } // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?>? ->
                exceptions!!
                    .defaultAuthenticationEntryPointFor(
                        LoginUrlAuthenticationEntryPoint("/login"),
                        MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                    )
            }

        return http.build()
    }

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .cors(Customizer.withDefaults())
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/test/**", "/login", "/api/login", "/api/2fa/**", "/merchant/invitation/validate", "/merchant/invitation/complete", "/password-reset/**", "/api/registration/**", "/v3/api-docs/**", "/swagger-ui/**")
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/oauth2/**",
                        "/.well-known/**",
                        "/error",
                        "/test/**",
                        "/",
                        "/api/login",
                        "/api/2fa/login",
                        "/api/2fa/verify",
                        "/api/2fa/resend",
                        "/api/2fa/refresh",
                        "/actuator/**",
                        "/merchant/invitation/validate",
                        "/merchant/invitation/complete",
                        "/password-reset/**",
                        "/api/registration/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/login/oauth2/code/**",
                        "/oauth2/authorization/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint { it.baseUri("/oauth2/authorization") }
                    .redirectionEndpoint { it.baseUri("/login/oauth2/code/*") }
                    .userInfoEndpoint {
                        it.oidcUserService(customOidcUserService)
                    }
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            }
            .formLogin { form ->
                form
                    .defaultSuccessUrl("/login-success", true)
                    .permitAll()
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }


    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder().build()
    }

    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider(userDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder)
        return authProvider
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun authenticationEventPublisher(applicationEventPublisher: ApplicationEventPublisher): AuthenticationEventPublisher {
        return DefaultAuthenticationEventPublisher(applicationEventPublisher)
    }

    @Bean
    fun tokenGenerator(
        jwtTokenCustomizer: OAuth2TokenCustomizer<JwtEncodingContext>,
        jwkSource: JWKSource<SecurityContext>
    ): OAuth2TokenGenerator<*> {
        val jwtEncoder: JwtEncoder = NimbusJwtEncoder(jwkSource)
        val jwtGenerator = JwtGenerator(jwtEncoder)
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer)

        val accessTokenGenerator = OAuth2AccessTokenGenerator()
        val refreshTokenGenerator = OAuth2RefreshTokenGenerator()

        return DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            accessTokenGenerator,
            refreshTokenGenerator
        )
    }
}