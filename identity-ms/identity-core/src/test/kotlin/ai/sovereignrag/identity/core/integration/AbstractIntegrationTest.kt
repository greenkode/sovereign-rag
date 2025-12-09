package ai.sovereignrag.identity.core.integration

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.RegistrationSource
import ai.sovereignrag.identity.core.entity.TrustLevel
import ai.sovereignrag.identity.core.entity.UserType
import ai.sovereignrag.identity.core.fixtures.OAuthClientBuilder
import ai.sovereignrag.identity.core.fixtures.UserBuilder
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

private val log = KotlinLogging.logger {}

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var userRepository: OAuthUserRepository

    @Autowired
    protected lateinit var clientRepository: OAuthRegisteredClientRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    open fun cleanDatabase() {
        userRepository.deleteAll()
        clientRepository.deleteAll()
        log.debug { "Database cleaned before test" }
    }

    protected fun createTestUser(
        email: String = "test@example.com",
        password: String = "TestPassword123!",
        firstName: String = "Test",
        lastName: String = "User",
        emailVerified: Boolean = false,
        registrationComplete: Boolean = false,
        authorities: MutableSet<String> = mutableSetOf("ROLE_USER"),
        merchantId: UUID? = null
    ): OAuthUser {
        val user = UserBuilder.default(
            id = null,
            email = email,
            password = passwordEncoder.encode(password),
            firstName = firstName,
            lastName = lastName,
            emailVerified = emailVerified,
            registrationComplete = registrationComplete,
            authorities = authorities,
            merchantId = merchantId
        )
        return userRepository.save(user)
    }

    protected fun createVerifiedUser(
        email: String = "verified@example.com",
        password: String = "TestPassword123!",
        merchantId: UUID? = null
    ): OAuthUser = createTestUser(
        email = email,
        password = password,
        emailVerified = true,
        registrationComplete = true,
        merchantId = merchantId
    )

    protected fun createAdminUser(
        email: String = "admin@example.com",
        password: String = "AdminPassword123!",
        merchantId: UUID? = null
    ): OAuthUser = createTestUser(
        email = email,
        password = password,
        emailVerified = true,
        registrationComplete = true,
        authorities = mutableSetOf(
            "ROLE_USER",
            "ROLE_ADMIN",
            "ROLE_SUPER_ADMIN",
            "ROLE_MERCHANT_ADMIN",
            "ROLE_MERCHANT_SUPER_ADMIN",
            "ROLE_MERCHANT_USER"
        ),
        merchantId = merchantId
    )

    protected fun createTestOrganization(
        domain: String = "example.com",
        name: String = "Test Organization"
    ): OAuthRegisteredClient {
        val client = OAuthClientBuilder.default(
            id = UUID.randomUUID(),
            clientId = UUID.randomUUID().toString(),
            clientName = name,
            domain = domain,
            clientSecret = passwordEncoder.encode("test_secret"),
            sandboxClientSecret = passwordEncoder.encode("sandbox_secret"),
            productionClientSecret = passwordEncoder.encode("production_secret")
        )
        return clientRepository.save(client)
    }

    protected fun createUserWithOrganization(
        email: String = "user@testorg.com",
        organizationDomain: String? = null,
        isAdmin: Boolean = false
    ): Pair<OAuthUser, OAuthRegisteredClient> {
        val domain = organizationDomain ?: email.substringAfter("@")
        val organization = createTestOrganization(domain = domain)
        val merchantId = organization.id

        val user = if (isAdmin) {
            createAdminUser(email = email, merchantId = merchantId)
        } else {
            createVerifiedUser(email = email, merchantId = merchantId)
        }

        return user to organization
    }

    companion object {
        @Container
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("identity_test")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.schemas") { "identity" }
            registry.add("spring.jpa.properties.hibernate.default_schema") { "identity" }
            registry.add("app.registration.verification-base-url") { "http://localhost:3000/auth/verify-email" }
            registry.add("app.password-reset.base-url") { "http://localhost:3000/auth/reset-password" }
            registry.add("app.merchant.invitation.base-url") { "http://localhost:3000/auth/onboard" }
            registry.add("app.registration.blocked-domains") { "gmail.com,yahoo.com" }
            registry.add("notification-ms.base-url") { "http://localhost:9081" }
            registry.add("jwt.issuer") { "http://localhost:9083" }
            registry.add("jwt.expiration") { "3600" }
        }
    }
}
