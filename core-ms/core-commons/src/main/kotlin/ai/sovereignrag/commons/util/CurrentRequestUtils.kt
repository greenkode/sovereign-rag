package ai.sovereignrag.commons.util

import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.enumeration.RequestContextAttributeName
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.RequestContextHolder

class CurrentRequestUtils {

    companion object {
        fun getChannel() = RequestContextHolder.getRequestAttributes()
            ?.getAttribute(RequestContextAttributeName.CHANNEL.name, SCOPE_REQUEST)?.let { it as ProcessChannel }
            ?: ProcessChannel.SYSTEM
    }
}