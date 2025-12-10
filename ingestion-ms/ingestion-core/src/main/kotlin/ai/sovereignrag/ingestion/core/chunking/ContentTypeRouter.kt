package ai.sovereignrag.ingestion.core.chunking

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.ingestion.core.chunking.strategy.CompositeSemanticChunker
import ai.sovereignrag.ingestion.core.chunking.strategy.FixedSizeChunkingStrategy
import ai.sovereignrag.ingestion.core.chunking.strategy.MarkdownChunkingStrategy
import ai.sovereignrag.ingestion.core.chunking.strategy.RecursiveCharacterSplitter
import ai.sovereignrag.ingestion.core.chunking.strategy.SentenceAwareChunkingStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ContentTypeRouter(
    private val fixedSizeStrategy: FixedSizeChunkingStrategy,
    private val sentenceAwareStrategy: SentenceAwareChunkingStrategy,
    private val recursiveStrategy: RecursiveCharacterSplitter,
    private val markdownStrategy: MarkdownChunkingStrategy,
    private val compositeStrategy: CompositeSemanticChunker
) : ChunkingStrategy {

    override val name: String = "content-type-router"
    override val description: String = "Routes documents to appropriate chunking strategy based on content type"

    private val mimeTypeMapping = mapOf(
        "text/markdown" to "markdown",
        "text/x-markdown" to "markdown",
        "text/html" to "recursive",
        "text/plain" to "sentence-aware",
        "text/csv" to "fixed-size",
        "application/pdf" to "composite",
        "application/json" to "recursive",
        "application/xml" to "recursive",
        "text/xml" to "recursive",
        "application/msword" to "sentence-aware",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "sentence-aware",
        "application/vnd.ms-excel" to "fixed-size",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "fixed-size",
        "application/vnd.ms-powerpoint" to "sentence-aware",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "sentence-aware",
        "text/x-python" to "code",
        "text/javascript" to "code",
        "application/javascript" to "code",
        "text/x-java" to "code",
        "text/x-kotlin" to "code",
        "text/x-c" to "code",
        "text/x-cpp" to "code",
        "text/x-go" to "code",
        "text/x-rust" to "code",
        "text/x-typescript" to "code"
    )

    private val strategyRegistry = mapOf(
        "fixed-size" to fixedSizeStrategy,
        "sentence-aware" to sentenceAwareStrategy,
        "recursive" to recursiveStrategy,
        "markdown" to markdownStrategy,
        "composite" to compositeStrategy,
        "code" to RecursiveCharacterSplitter(separators = RecursiveCharacterSplitter.CODE_SEPARATORS)
    )

    private var defaultStrategy: ChunkingStrategy = sentenceAwareStrategy
    private var defaultConfig = ChunkingConfig()

    fun setDefaultStrategy(strategyName: String) {
        defaultStrategy = strategyRegistry[strategyName]
            ?: throw IllegalArgumentException("Unknown strategy: $strategyName")
    }

    fun setDefaultConfig(config: ChunkingConfig) {
        defaultConfig = config
    }

    fun registerMimeType(mimeType: String, strategyName: String) {
        if (strategyRegistry.containsKey(strategyName)) {
            (mimeTypeMapping as MutableMap)[mimeType] = strategyName
        } else {
            throw IllegalArgumentException("Unknown strategy: $strategyName")
        }
    }

    override fun supports(mimeType: String): Boolean = true

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        val strategy = selectStrategy(document.mimeType)
        log.debug { "Selected strategy '${strategy.name}' for MIME type '${document.mimeType}'" }

        return when (strategy) {
            is FixedSizeChunkingStrategy -> strategy.chunk(document, config)
            is SentenceAwareChunkingStrategy -> strategy.chunk(document, config)
            is RecursiveCharacterSplitter -> strategy.chunk(document, config, getCodeSeparators(document.mimeType))
            is MarkdownChunkingStrategy -> strategy.chunk(document, config)
            is CompositeSemanticChunker -> strategy.chunk(document, config)
            else -> strategy.chunk(document)
        }
    }

    fun selectStrategy(mimeType: String): ChunkingStrategy {
        val strategyName = mimeTypeMapping[mimeType]
            ?: mimeTypeMapping.entries.find { mimeType.startsWith(it.key.substringBefore("/")) }?.value

        return strategyName?.let { strategyRegistry[it] } ?: defaultStrategy
    }

    private fun getCodeSeparators(mimeType: String): List<String> = when {
        mimeType.contains("python") -> listOf(
            "\nclass ",
            "\ndef ",
            "\n\ndef ",
            "\n\n",
            "\n",
            " ",
            ""
        )
        mimeType.contains("java") || mimeType.contains("kotlin") -> listOf(
            "\nclass ",
            "\ninterface ",
            "\nfun ",
            "\npublic ",
            "\nprivate ",
            "\n\n",
            "\n",
            " ",
            ""
        )
        mimeType.contains("javascript") || mimeType.contains("typescript") -> listOf(
            "\nfunction ",
            "\nclass ",
            "\nconst ",
            "\nlet ",
            "\nexport ",
            "\n\n",
            "\n",
            " ",
            ""
        )
        else -> RecursiveCharacterSplitter.CODE_SEPARATORS
    }

    fun getAvailableStrategies(): Set<String> = strategyRegistry.keys

    fun getMimeTypeMapping(): Map<String, String> = mimeTypeMapping.toMap()
}
