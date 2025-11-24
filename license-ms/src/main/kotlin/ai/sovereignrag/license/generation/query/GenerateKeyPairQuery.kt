package ai.sovereignrag.license.generation.query

data class GenerateKeyPairQuery(
    val placeholder: Boolean = true
)

data class GenerateKeyPairResult(
    val publicKey: String,
    val privateKey: String
)
