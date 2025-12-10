package ai.sovereignrag.ingestion.core.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import javax.sql.DataSource

@Configuration
class QuartzDataSourceConfig {

    @Bean
    @QuartzDataSource
    fun quartzDataSource(@Qualifier("primaryDataSource") primaryDataSource: DataSource): DataSource {
        return primaryDataSource
    }
}
