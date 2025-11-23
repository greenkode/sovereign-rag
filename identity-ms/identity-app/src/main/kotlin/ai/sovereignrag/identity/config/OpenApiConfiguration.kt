package ai.sovereignrag.identity.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class OpenApiConfiguration {

    @Bean
    @Profile("sandbox", "local")
    @ConditionalOnProperty(
        name = ["springdoc.api-docs.enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun developmentOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Sovereign RAG Identity Service API - Development")
                    .version("1.0.0")
                    .description(
                        """
                        # Sovereign RAG Identity Service API - Development Environment

                        Welcome to the Sovereign RAG Identity Service API documentation. This service provides comprehensive
                        identity and access management functionality for the Sovereign RAG financial platform, including
                        user authentication, authorization, merchant management, and security services.

                        ## Key Features
                        - **User Authentication**: Secure login with multi-factor authentication support
                        - **OAuth2 Authorization**: Standards-compliant OAuth2/OpenID Connect implementation
                        - **Merchant Management**: Complete merchant onboarding and lifecycle management
                        - **Role-Based Access Control**: Granular permission and role management
                        - **Security Services**: Password reset, account lockout, and security monitoring
                        - **Admin Operations**: Administrative tools for user and merchant management
                        - **Settings Management**: User preferences and configuration management

                        ## Authentication Flows
                        - **Authorization Code Flow**: For interactive user authentication
                        - **Client Credentials Flow**: For service-to-service communication
                        - **Two-Factor Authentication**: SMS and TOTP-based 2FA support
                        - **Password Reset**: Secure password recovery workflows

                        ## Security Features
                        - JWT token-based authentication
                        - Rate limiting and brute force protection
                        - Account lockout mechanisms
                        - Audit logging and monitoring
                        - Session management

                        ## Environment
                        This documentation covers the **complete Identity API** available in development and sandbox environments.
                        """.trimIndent()
                    )
                    .contact(
                        Contact()
                            .name("Sovereign RAG Development Team")
                            .email("dev@sovereignrag.ai")
                            .url("https://sovereignrag.ai")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                            .url("https://sovereignrag.ai/license")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:9093")
                    .description("Local Development")
            )
    }

    @Bean
    @Profile("production")
    @ConditionalOnProperty(
        name = ["springdoc.api-docs.enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun productionOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Sovereign RAG Authentication API")
                    .version("1.0.0")
                    .description(
                        """
                        # Sovereign RAG Authentication API - Official Documentation

                        Welcome to the official Sovereign RAG Authentication API. This API provides secure authentication
                        and authorization services for merchants and partners integrating with the Sovereign RAG platform.

                        ## Overview
                        The Sovereign RAG Authentication API implements industry-standard OAuth2 and OpenID Connect protocols
                        to provide secure, scalable authentication and authorization services. This API is designed
                        for partners who need to authenticate users and obtain access tokens for Sovereign RAG services.

                        ## Supported Flows
                        - **Client Credentials Grant**: For service-to-service authentication
                        - **Authorization Code Flow**: For user-interactive authentication (where applicable)

                        ## Key Features
                        - Standards-compliant OAuth2/OpenID Connect implementation
                        - JWT-based access tokens
                        - Secure token refresh mechanisms
                        - Rate limiting and security monitoring
                        - Comprehensive audit logging

                        ## Authentication Process
                        1. **Client Registration**: Obtain client credentials from Sovereign RAG
                        2. **Token Request**: Use client credentials to obtain access tokens
                        3. **API Access**: Use access tokens to access Sovereign RAG services
                        4. **Token Refresh**: Refresh tokens before expiration

                        ## Security Requirements
                        - All communication must use HTTPS
                        - Client credentials must be securely stored
                        - Implement proper token storage and handling
                        - Follow OAuth2 security best practices

                        ## Rate Limits
                        - Token requests: 100 requests per minute per client
                        - Authentication attempts: Subject to security policies

                        ## Support
                        For integration support and client credential provisioning, contact our partnership team
                        at partnerships@sovereignrag.ai or visit our developer portal.
                        """.trimIndent()
                    )
                    .contact(
                        Contact()
                            .name("Sovereign RAG Partnership Team")
                            .email("partnerships@sovereignrag.ai")
                            .url("https://developers.sovereignrag.ai")
                    )
                    .license(
                        License()
                            .name("Commercial License")
                            .url("https://sovereignrag.ai/terms")
                    )
            )
            .addServersItem(
                Server()
                    .url("https://identity.sovereignrag.ai")
                    .description("Production Environment")
            )
    }

    @Bean
    @Profile("production")
    @ConditionalOnProperty(
        name = ["springdoc.api-docs.enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun authenticationApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("authentication")
            .displayName("Authentication API")
            .pathsToMatch("/oauth2/**", "/login/**")
            .build()
    }
}