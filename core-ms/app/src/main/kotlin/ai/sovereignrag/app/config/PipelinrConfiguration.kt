package ai.sovereignrag.app.config

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.CommandHandlers
import an.awesome.pipelinr.Pipeline
import an.awesome.pipelinr.Pipelinr
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelinrConfiguration {

    @Bean
    fun pipeline(commandHandlers: ObjectProvider<Command.Handler<*, *>>): Pipeline {
        val handlers = CommandHandlers { -> commandHandlers.orderedStream() }
        return Pipelinr()
            .with(handlers)
    }
}