package ai.sovereignrag.testcommons.fixtures

import java.time.Instant
import java.util.UUID

object TestDataFixtures {

    fun randomId(): String = UUID.randomUUID().toString()

    fun randomEmail(): String = "test-${UUID.randomUUID().toString().take(8)}@test.com"

    fun fixedTimestamp(): Instant = Instant.parse("2024-01-15T10:00:00Z")

    fun randomName(prefix: String = "Test"): String = "$prefix-${UUID.randomUUID().toString().take(8)}"
}
