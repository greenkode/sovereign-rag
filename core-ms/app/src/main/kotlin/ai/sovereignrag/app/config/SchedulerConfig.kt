package ai.sovereignrag.app.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

@Configuration
@EnableScheduling
class SchedulerConfig
