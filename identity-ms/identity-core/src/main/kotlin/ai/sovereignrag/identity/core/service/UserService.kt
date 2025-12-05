package ai.sovereignrag.identity.core.service

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class UserService(
    private val userRepository: OAuthUserRepository
) {

    fun getCurrentUser(): OAuthUser {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken) {
            throw InvalidRequestException("Invalid authentication")
        }

        val userId = try {
            UUID.fromString(authentication.name)
        } catch (e: IllegalArgumentException) {
            throw InvalidRequestException("Invalid user ID format")
        }

        return userRepository.findById(userId)
            .orElseThrow { RecordNotFoundException("User not found") }
    }

    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken) {
            throw InvalidRequestException("Invalid authentication")
        }

        return try {
            UUID.fromString(authentication.name)
        } catch (e: IllegalArgumentException) {
            throw InvalidRequestException("Invalid user ID format")
        }
    }

    fun getCurrentUserRoles(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication !is JwtAuthenticationToken) {
            throw InvalidRequestException("Invalid authentication")
        }

        return authentication.token.getClaimAsStringList("authorities") ?: emptyList()
    }
}