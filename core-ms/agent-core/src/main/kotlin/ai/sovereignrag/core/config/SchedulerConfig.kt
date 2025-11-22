package ai.sovereignrag.core.config

import nl.compilot.ai.chat.service.ChatSessionManager
import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

private val logger = KotlinLogging.logger {}

@Configuration
@EnableScheduling
class SchedulerConfig(
    private val chatSessionManager: ChatSessionManager
) {

    /**
     * Clean up expired chat sessions every 5 minutes
     * Sessions are considered expired after 30 minutes of inactivity
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    fun cleanupExpiredSessions() {
        logger.debug { "Running scheduled session cleanup..." }
        chatSessionManager.cleanupExpiredChatSessions(timeoutMinutes = 30)
    }
}
