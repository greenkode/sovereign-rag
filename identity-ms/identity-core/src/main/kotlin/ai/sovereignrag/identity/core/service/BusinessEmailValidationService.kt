package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class BusinessEmailValidationService(
    private val messageService: MessageService,
    @Value("\${app.registration.blocked-domains:gmail.com,yahoo.com,hotmail.com,outlook.com,live.com,aol.com,icloud.com,mail.com,protonmail.com,yandex.com}")
    private val blockedDomainsConfig: String
) {
    private val blockedDomains: Set<String> by lazy {
        blockedDomainsConfig.split(",").map { it.trim().lowercase() }.toSet()
    }

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateBusinessEmail(email: String) {
        val normalizedEmail = email.trim().lowercase()

        if (!emailRegex.matches(normalizedEmail)) {
            throw ClientException(messageService.getMessage("oauth.error.invalid_email"))
        }

        val domain = normalizedEmail.substringAfter("@")

        if (blockedDomains.contains(domain)) {
            log.info { "Blocked personal email domain: $domain" }
            throw ClientException(messageService.getMessage("oauth.error.business_email_required"))
        }
    }

    fun isBusinessEmail(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()

        if (!emailRegex.matches(normalizedEmail)) {
            return false
        }

        val domain = normalizedEmail.substringAfter("@")
        return !blockedDomains.contains(domain)
    }
}
