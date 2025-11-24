package ai.sovereignrag.license

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LicenseManagementApplication

fun main(args: Array<String>) {
    runApplication<LicenseManagementApplication>(*args)
}
