package ai.sovereignrag.identity.core.config

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.Notification
import an.awesome.pipelinr.Pipeline
import an.awesome.pipelinr.Pipelinr
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelinrConfig {
    
    @Bean
    fun pipeline(
        commandHandlers: ObjectProvider<Command.Handler<*, *>>,
        notificationHandlers: ObjectProvider<Notification.Handler<*>>
    ): Pipeline {
        return Pipelinr()
            .with(commandHandlers::stream)
            .with(notificationHandlers::stream)
    }
}