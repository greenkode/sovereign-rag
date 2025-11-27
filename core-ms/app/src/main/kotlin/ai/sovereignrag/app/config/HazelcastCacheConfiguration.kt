package ai.sovereignrag.app.config

import ai.sovereignrag.commons.cache.SrCache
import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.cache.HazelcastCacheManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
@EnableCaching
@ConditionalOnClass(HazelcastInstance::class)
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "hazelcast", matchIfMissing = false)
class HazelcastCacheConfiguration {

    @Value("\${hazelcast.cluster.name:core-cluster}")
    private lateinit var clusterName: String

    @Value("\${hazelcast.port:5701}")
    private var port: Int = 5701

    @Value("\${hazelcast.members:127.0.0.1:5701}")
    private lateinit var members: String

    @Bean
    fun hazelcastInstance(): HazelcastInstance {
        log.info { "Initializing Hazelcast instance for cluster: $clusterName" }

        val config = Config().apply {
            this.clusterName = this@HazelcastCacheConfiguration.clusterName

            networkConfig.apply {
                this.port = this@HazelcastCacheConfiguration.port
                join.tcpIpConfig.apply {
                    isEnabled = true
                    this@HazelcastCacheConfiguration.members.split(",").forEach { member -> addMember(member.trim()) }
                }
                join.multicastConfig.isEnabled = false
            }

            SrCache.entries.forEach { cache ->
                addMapConfig(
                    MapConfig(cache.cacheName).apply {
                        timeToLiveSeconds = cache.computeTtl(cache.timeUnit, cache.ttl).toSeconds().toInt()
                        maxIdleSeconds = cache.computeTtl(cache.timeUnit, cache.ttl).toSeconds().toInt()
                    }
                )
            }
        }

        return Hazelcast.newHazelcastInstance(config)
    }

    @Bean
    fun cacheManager(hazelcastInstance: HazelcastInstance): CacheManager {
        log.info { "Initializing Hazelcast cache manager" }
        return HazelcastCacheManager(hazelcastInstance)
    }
}
