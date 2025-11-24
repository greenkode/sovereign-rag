package ai.sovereignrag.identity.config

import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver
import com.giffing.bucket4j.spring.boot.starter.config.cache.hazelcast.HazelcastCacheResolver
import com.hazelcast.core.HazelcastInstance
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

private val log = KotlinLogging.logger {}

@Configuration
class Bucket4jHazelcastConfig {

    @Autowired(required = false)
    private var hazelcastInstance: HazelcastInstance? = null

    @Bean
    @Primary
    fun bucket4jCacheResolver(): SyncCacheResolver {
        if (hazelcastInstance != null) {
            log.info { "Configuring Bucket4j with Hazelcast" }
            return HazelcastCacheResolver(hazelcastInstance!!, false)
        } else {
            log.warn { "Hazelcast instance not available for Bucket4j" }
            throw IllegalStateException("Hazelcast instance is required for rate limiting")
        }
    }
}