package ai.sovereignrag.ingestion.app.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

private val log = KotlinLogging.logger {}

@Configuration
@EnableAsync
class AsyncConfiguration {

    @Bean("qualityEvaluationExecutor")
    fun qualityEvaluationExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("quality-eval-")
        executor.setRejectedExecutionHandler { runnable, _ ->
            log.warn { "Quality evaluation task rejected, running in caller thread" }
            runnable.run()
        }
        executor.initialize()
        return executor
    }
}
