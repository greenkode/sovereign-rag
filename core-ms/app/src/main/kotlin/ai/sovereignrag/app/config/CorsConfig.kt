package ai.sovereignrag.app.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import kotlin.apply
import kotlin.collections.map
import kotlin.text.split
import kotlin.text.trim

@Configuration
@EnableConfigurationProperties(SovereignRagProperties::class)
class CorsConfig(
    private val properties: SovereignRagProperties
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            val origins = properties.cors.allowedOrigins.split(",").map { it.trim() }

            // When allowCredentials is true, use allowedOriginPatterns instead of allowedOrigins
            if (properties.cors.allowCredentials) {
                allowedOriginPatterns = origins
            } else {
                allowedOrigins = origins
            }

            allowedMethods = properties.cors.allowedMethods.split(",").map { it.trim() }
            allowedHeaders = properties.cors.allowedHeaders.split(",").map { it.trim() }
            allowCredentials = properties.cors.allowCredentials
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }

        return CorsFilter(source)
    }
}
