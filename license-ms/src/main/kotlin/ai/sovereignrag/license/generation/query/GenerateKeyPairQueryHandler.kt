package ai.sovereignrag.license.generation.query

import ai.sovereignrag.license.service.LicenseGenerationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class GenerateKeyPairQueryHandler(
    private val licenseGenerationService: LicenseGenerationService
) {

    fun handle(query: GenerateKeyPairQuery): GenerateKeyPairResult {
        log.info { "Generating new RSA key pair" }

        val keyPair = licenseGenerationService.generateKeyPair()

        return GenerateKeyPairResult(
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey
        )
    }
}
