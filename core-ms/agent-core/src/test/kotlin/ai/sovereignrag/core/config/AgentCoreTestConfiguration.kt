package ai.sovereignrag.core.config

import ai.sovereignrag.testcommons.config.TestPipelineConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(TestPipelineConfiguration::class)
class AgentCoreTestConfiguration
