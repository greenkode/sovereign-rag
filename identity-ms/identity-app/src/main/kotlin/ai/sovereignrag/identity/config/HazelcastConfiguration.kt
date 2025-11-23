package ai.sovereignrag.identity.config

import ai.sovereignrag.identity.commons.cache.IdentityCache
import com.hazelcast.config.Config
import com.hazelcast.config.EvictionConfig
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizePolicy
import com.hazelcast.config.NetworkConfig
import com.hazelcast.config.SerializationConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.cache.HazelcastCacheManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "hazelcast")
class HazelcastConfiguration(private val environment: Environment) {

    @Bean
    fun hazelcastInstance(): HazelcastInstance {
        val config = Config()

        config.instanceName = "identity-hazelcast-instance"
        config.clusterName = environment.getProperty("hazelcast.cluster.name", "identity-cluster")

        configureNetworking(config)
        configureSerialization(config)
        configureMaps(config)

        return Hazelcast.newHazelcastInstance(config)
    }

    @Bean
    fun cacheManager(hazelcastInstance: HazelcastInstance): CacheManager {
        return HazelcastCacheManager(hazelcastInstance)
    }

    private fun configureNetworking(config: Config) {
        val networkConfig = NetworkConfig()

        networkConfig.port = environment.getProperty("hazelcast.port", "5702").toInt()
        networkConfig.setPortAutoIncrement(true)

        val members = environment.getProperty("hazelcast.members", "127.0.0.1:5702")
            .split(",")
            .map { it.trim() }

        networkConfig.join.multicastConfig.isEnabled = false
        networkConfig.join.tcpIpConfig.isEnabled = true
        networkConfig.join.tcpIpConfig.members = members

        config.networkConfig = networkConfig
    }

    private fun configureSerialization(config: Config) {
        val serializationConfig = SerializationConfig()

        serializationConfig.isCheckClassDefErrors = false
        serializationConfig.isAllowUnsafe = true

        config.serializationConfig = serializationConfig
    }

    private fun configureMaps(config: Config) {
        val defaultMapConfig = MapConfig()
        defaultMapConfig.name = "default"

        val evictionConfig = EvictionConfig()
        evictionConfig.evictionPolicy = EvictionPolicy.LRU
        evictionConfig.maxSizePolicy = MaxSizePolicy.PER_NODE
        evictionConfig.size = 10000

        defaultMapConfig.evictionConfig = evictionConfig
        defaultMapConfig.timeToLiveSeconds = 300
        defaultMapConfig.maxIdleSeconds = 600
        defaultMapConfig.isStatisticsEnabled = true

        config.addMapConfig(defaultMapConfig)

        configureIdentityCaches(config)
        configureCoreCaches(config)
        configureRateLimitCache(config)
    }

    private fun configureIdentityCaches(config: Config) {
        IdentityCache.entries.forEach { cache ->
            val ttlSeconds = cache.timeUnit.toSeconds(cache.ttl).toInt()
            val idleSeconds = (ttlSeconds / 2).coerceAtLeast(60)

            configureMapWithTtl(
                config,
                cache.cacheName,
                ttlSeconds,
                idleSeconds,
                5000
            )
        }
    }

    private fun configureCoreCaches(config: Config) {
        configureMapWithTtl(config, "KycUser", 1800, 900, 5000)
        configureMapWithTtl(config, "MerchantDetails", 3600, 1800, 5000)
        configureMapWithTtl(config, "UserDetails", 1800, 900, 5000)
    }

    private fun configureRateLimitCache(config: Config) {
        val rateLimitConfig = MapConfig("rate-limit-buckets")
        rateLimitConfig.timeToLiveSeconds = 3600
        rateLimitConfig.maxIdleSeconds = 1800
        rateLimitConfig.isStatisticsEnabled = true

        val evictionConfig = EvictionConfig()
        evictionConfig.evictionPolicy = EvictionPolicy.LRU
        evictionConfig.maxSizePolicy = MaxSizePolicy.PER_NODE
        evictionConfig.size = 10000
        rateLimitConfig.evictionConfig = evictionConfig

        config.addMapConfig(rateLimitConfig)
    }

    private fun configureMapWithTtl(
        config: Config,
        name: String,
        ttl: Int,
        idle: Int,
        size: Int
    ) {
        val mapConfig = MapConfig(name)
        mapConfig.timeToLiveSeconds = ttl
        mapConfig.maxIdleSeconds = idle
        mapConfig.isStatisticsEnabled = true

        val evictionConfig = EvictionConfig()
        evictionConfig.evictionPolicy = EvictionPolicy.LRU
        evictionConfig.maxSizePolicy = MaxSizePolicy.PER_NODE
        evictionConfig.size = size
        mapConfig.evictionConfig = evictionConfig

        config.addMapConfig(mapConfig)
    }
}
