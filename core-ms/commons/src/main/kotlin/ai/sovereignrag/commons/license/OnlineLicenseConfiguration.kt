package ai.sovereignrag.commons.license

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OnlineLicenseProperties::class)
class OnlineLicenseConfiguration
