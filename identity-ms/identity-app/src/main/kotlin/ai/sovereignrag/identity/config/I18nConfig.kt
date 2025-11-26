package ai.sovereignrag.identity.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

private val log = KotlinLogging.logger {}

@Configuration
class I18nConfig {

    @Bean
    fun messageSource(): MessageSource = ReloadableResourceBundleMessageSource().apply {
        val resolver = PathMatchingResourcePatternResolver()
        val baseNames = resolver.getResources("classpath*:messages-*.properties")
            .mapNotNull { resource ->
                resource.filename
                    ?.removeSuffix(".properties")
                    ?.let { "classpath:$it" }
            }
            .also { log.info { "Loaded i18n message sources: $it" } }

        setBasenames(*baseNames.toTypedArray())
        setDefaultEncoding("UTF-8")
        setUseCodeAsDefaultMessage(true)
    }

    @Bean
    fun localeResolver(): LocaleResolver = AcceptHeaderLocaleResolver().apply {
        setDefaultLocale(Locale.ENGLISH)
    }
}
