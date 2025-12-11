package ai.sovereignrag.core.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan(basePackages = ["ai.sovereignrag.core.rag.memory"])
@EnableJpaRepositories(basePackages = ["ai.sovereignrag.core.rag.memory"])
class TestApplication
