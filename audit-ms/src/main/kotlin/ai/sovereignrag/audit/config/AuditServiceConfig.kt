package ai.sovereignrag.audit.config

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.CommandHandlers
import an.awesome.pipelinr.Notification
import an.awesome.pipelinr.NotificationHandlers
import an.awesome.pipelinr.Pipeline
import an.awesome.pipelinr.Pipelinr
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuditServiceConfig {

    @Bean
    fun pipeline(
        commandHandlers: ObjectProvider<Command.Handler<*, *>>,
        notificationHandlers: ObjectProvider<Notification.Handler<*>>,
        middlewares: ObjectProvider<Command.Middleware>
    ): Pipeline {
        return Pipelinr()
            .with(CommandHandlers { commandHandlers.stream() })
            .with(NotificationHandlers { notificationHandlers.stream() })
            .with(Command.Middlewares { middlewares.orderedStream() })
    }
}