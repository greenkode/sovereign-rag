package ai.sovereignrag.knowledgebase.config

import ai.sovereignrag.testcommons.config.AbstractDataJpaIntegrationTest
import org.springframework.context.annotation.Import

@Import(IntegrationTestConfiguration::class)
abstract class AbstractIntegrationTest : AbstractDataJpaIntegrationTest()
