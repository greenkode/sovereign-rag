# JWT Authentication Implementation Guide

## Status: PARTIAL - Dependencies and JWT Provider Complete

## ✅ Completed Steps

### 1. Dependencies Added (core-ai/pom.xml:78-112)
- Spring Security (core, web, config) - version 6.2.3
- JJWT (api, impl, jackson) - version 0.12.3

### 2. JWT Token Provider Created
**File:** `core-ai/src/main/kotlin/ai/sovereignrag/security/JwtTokenProvider.kt`
- Creates JWT tokens with tenant ID as subject
- Validates tokens
- Extracts tenant ID from tokens
- Configurable expiration (default 1 hour)

## 🔨 Remaining Implementation Steps

### 3. Create Authentication Controller

**File:** `core-ai/src/main/kotlin/ai/sovereignrag/security/api/AuthController.kt`

```kotlin
package ai.sovereignrag.security.api

import mu.KotlinLogging
import ai.sovereignrag.security.JwtTokenProvider
import ai.sovereignrag.tenant.service.TenantRegistry
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val tenantRegistry: TenantRegistry
) {

    @PostMapping("/authenticate")
    fun authenticate(@RequestBody request: AuthRequest): AuthResponse {
        logger.info { "Authentication request for tenant: ${request.tenantId}" }

        // Validate tenant credentials using existing TenantRegistry
        val tenant = tenantRegistry.validateTenant(request.tenantId, request.apiKey)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        // Generate JWT token
        val token = jwtTokenProvider.createToken(tenant.id)
        val expiresIn = jwtTokenProvider.getExpirationTimeInSeconds()

        logger.info { "JWT token generated for tenant: ${tenant.id}" }

        return AuthResponse(
            token = token,
            expiresIn = expiresIn.toInt(),
            tenantId = tenant.id
        )
    }
}

data class AuthRequest(
    val tenantId: String,
    val apiKey: String
)

data class AuthResponse(
    val token: String,
    val expiresIn: Int,
    val tenantId: String
)
```

### 4. Create JWT Authentication Filter

**File:** `core-ai/src/main/kotlin/ai/sovereignrag/security/JwtAuthenticationFilter.kt`

```kotlin
package ai.sovereignrag.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import ai.sovereignrag.commons.tenant.TenantContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = resolveToken(request)

            if (token != null && jwtTokenProvider.validateToken(token)) {
                val tenantId = jwtTokenProvider.getTenantId(token)

                // Set tenant context for this request
                TenantContext.setCurrentTenant(tenantId)

                // Set authentication in Spring Security context
                val authentication = UsernamePasswordAuthenticationToken(
                    tenantId, null, emptyList()
                )
                SecurityContextHolder.getContext().authentication = authentication

                logger.debug { "JWT authentication successful for tenant: $tenantId" }
            }

            filterChain.doFilter(request, response)
        } finally {
            // Always clear tenant context to prevent memory leaks
            TenantContext.clear()
            SecurityContextHolder.clearContext()
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}
```

### 5. Create Security Configuration

**File:** `core-ai/src/main/kotlin/ai/sovereignrag/security/SecurityConfig.kt`

```kotlin
package ai.sovereignrag.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // TODO: Configure these properly for production
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("Authorization")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
```

### 6. Update application.yml

**File:** `app/src/main/resources/application.yml`

Add these properties:

```yaml
sovereignrag:
  jwt:
    # IMPORTANT: Change this in production! Minimum 256 bits (32 characters)
    secret: ${JWT_SECRET:please-change-this-secret-key-in-production-min-256-bits}
    expiration: 3600000 # 1 hour in milliseconds
```

### 7. Update WordPress Plugin - PHP

**File:** `wordpress-plugin/sovereign-rag-plugin/includes/chat-widget.php`

Update the `enqueue_scripts()` method:

```php
public function enqueue_scripts() {
    // ... existing code ...

    // Get JWT token from backend (server-side authentication)
    $jwt_token = $this->get_jwt_token();

    wp_localize_script('sovereign-rag-chat-widget', 'sovereignragChat', array(
        'apiUrl' => get_option('sovereignrag_ai_frontend_api_url', 'http://localhost:8000'),
        'jwtToken' => $jwt_token, // Pass JWT instead of sensitive credentials
        // REMOVED for security: 'tenantId', 'apiKey'
        'ajaxUrl' => admin_url('admin-ajax.php'),
        'nonce' => wp_create_nonce('sovereignrag_chat_nonce'),
        // ... other settings ...
    ));
}

private function get_jwt_token() {
    // Check if we have a cached valid token (cache for 90% of lifetime)
    $cached_token = get_transient('sovereignrag_jwt_token');
    if ($cached_token) {
        return $cached_token;
    }

    // Authenticate with backend to get new token
    $api_url = get_option('sovereignrag_ai_frontend_api_url', 'http://localhost:8000');
    $tenant_id = get_option('sovereignrag_ai_tenant_id', '');
    $api_key = get_option('sovereignrag_ai_api_key', '');

    if (empty($tenant_id) || empty($api_key)) {
        error_log('Sovereign RAG: Missing tenant credentials');
        return null;
    }

    $response = wp_remote_post($api_url . '/api/auth/authenticate', array(
        'headers' => array('Content-Type' => 'application/json'),
        'body' => json_encode(array(
            'tenantId' => $tenant_id,
            'apiKey' => $api_key
        )),
        'timeout' => 10
    ));

    if (is_wp_error($response)) {
        error_log('Sovereign RAG: Authentication failed - ' . $response->get_error_message());
        return null;
    }

    $body = json_decode(wp_remote_retrieve_body($response), true);
    $token = $body['token'] ?? null;
    $expires_in = $body['expiresIn'] ?? 3600;

    // Cache token for 90% of its lifetime
    if ($token) {
        set_transient('sovereignrag_jwt_token', $token, $expires_in * 0.9);
    }

    return $token;
}
```

### 8. Update Chat Widget JavaScript

**File:** `wordpress-plugin/sovereign-rag-plugin/assets/js/chat-widget.js`

Update all AJAX calls to use JWT token in Authorization header:

```javascript
// Line ~491 - startChatSession
function startChatSession() {
    $.ajax({
        url: apiUrl,
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + sovereignragChat.jwtToken,
            'Content-Type': 'application/json'
        },
        // ... rest of config
    });
}

// Line ~543 - sendChatMessage
function sendChatMessage(message) {
    $.ajax({
        url: apiUrl,
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + sovereignragChat.jwtToken,
            'Content-Type': 'application/json'
        },
        // ... rest of config
    });
}

// Update ALL other AJAX calls similarly (escalate, close, autocomplete, etc.)
```

## 🔐 Security Improvements

1. **API Key Protection**: API key never exposed to browser
2. **Short-lived Tokens**: JWT expires after 1 hour
3. **Automatic Refresh**: WordPress gets new tokens automatically
4. **Tenant Isolation**: JWT validates tenant on every request
5. **Stateless Auth**: No session management needed

## 🧪 Testing Checklist

- [ ] Authentication endpoint returns valid JWT
- [ ] JWT token validates correctly
- [ ] Protected endpoints reject requests without JWT
- [ ] Protected endpoints accept valid JWT
- [ ] Expired tokens are rejected
- [ ] WordPress plugin fetches and caches JWT
- [ ] Chat widget uses JWT for all requests
- [ ] CORS is configured correctly
- [ ] Tenant context is set properly from JWT
- [ ] Old header-based auth still works (backward compatibility)

## 🚀 Deployment Notes

1. **JWT Secret**: MUST be changed in production
   - Minimum 256 bits (32 characters)
   - Store in environment variable
   - Use strong random generator

2. **CORS**: Configure allowed origins properly
   - Replace `*` with specific WordPress domains
   - Use `allowedOriginPatterns` for dynamic subdomains

3. **HTTPS**: All communication MUST use HTTPS in production

4. **Token Expiration**: Adjust based on your needs
   - Current: 1 hour
   - Consider: Refresh tokens for longer sessions

## 📝 Migration Strategy

1. Deploy backend changes first
2. Keep existing header auth working (backward compatibility)
3. Update WordPress plugin
4. Test thoroughly
5. Monitor for auth failures
6. Eventually deprecate header-based auth

## Current Implementation Status

- ✅ Dependencies added
- ✅ JWT Provider created
- ⏳ Auth Controller - **TODO**
- ⏳ JWT Filter - **TODO**
- ⏳ Security Config - **TODO**
- ⏳ Application config - **TODO**
- ⏳ WordPress PHP updates - **TODO**
- ⏳ JavaScript updates - **TODO**
