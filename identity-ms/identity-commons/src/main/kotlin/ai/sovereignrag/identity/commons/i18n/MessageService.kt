package ai.sovereignrag.identity.commons.i18n

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class MessageService(private val messageSource: MessageSource) {

    fun getMessage(key: String, vararg args: Any): String =
        messageSource.getMessage(key, args, LocaleContextHolder.getLocale())
}
