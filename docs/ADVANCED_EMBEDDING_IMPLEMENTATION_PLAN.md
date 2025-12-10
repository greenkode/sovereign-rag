# Advanced Embedding Strategies Implementation Plan

## Executive Summary

This plan outlines the implementation of sophisticated context-aware embedding strategies for Sovereign RAG. The goal is to transform the current simple fixed-size chunking approach into a multi-strategy, semantically-aware system that truly understands document context.

## Current State Analysis

### What Exists
- **Simple Chunking**: Fixed 1000-character chunks with 200-character overlap
- **Multi-Provider Embeddings**: OpenAI, HuggingFace, Ollama support via LangChain4j
- **PgVector Storage**: Per-knowledge-base tables with HNSW indexing
- **Job-Based Processing**: Async processing with progress tracking

### Key Gaps
- No semantic boundary detection
- No document structure awareness (headers, paragraphs, code blocks)
- No content-type-specific chunking
- No chunk quality evaluation
- No strategy optimization or A/B testing

---

## Phase 1: Core Chunking Strategy Framework

### 1.1 Strategy Interface Design

**Location**: `commons/src/main/kotlin/ai/sovereignrag/commons/chunking/`

```kotlin
interface ChunkingStrategy {
    val name: String
    val description: String

    fun chunk(document: Document): List<DocumentChunk>
    fun supports(mimeType: String): Boolean
}

data class Document(
    val content: String,
    val mimeType: String,
    val metadata: Map<String, Any>,
    val structure: DocumentStructure?
)

data class DocumentChunk(
    val content: String,
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata,
    val parentChunkId: String? = null,
    val childChunkIds: List<String> = emptyList()
)

data class ChunkMetadata(
    val sourceType: SourceType,
    val headingHierarchy: List<String> = emptyList(),
    val sectionTitle: String? = null,
    val contentType: ContentType = ContentType.PROSE,
    val language: String = "en",
    val confidence: Double = 1.0
)

enum class ContentType {
    PROSE, CODE, TABLE, LIST, HEADER, QUOTE, METADATA
}
```

### 1.2 Base Strategy Implementations

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `FixedSizeChunkingStrategy` | Current approach (refactored) | Fallback, simple documents |
| `SentenceAwareChunkingStrategy` | Respects sentence boundaries | Articles, documentation |
| `ParagraphChunkingStrategy` | Splits on paragraph boundaries | Structured text |
| `RecursiveCharacterSplitter` | Hierarchical splitting with separators | Complex documents |
| `MarkdownChunkingStrategy` | Preserves markdown structure | Technical docs, README files |
| `CodeAwareChunkingStrategy` | Understands code blocks/functions | Code repositories |

---

## Phase 2: Semantic Chunking Strategies

### 2.1 Embedding-Based Semantic Chunking

**Concept**: Use embeddings to detect semantic boundaries by measuring similarity between adjacent segments.

```kotlin
interface SemanticChunkingStrategy : ChunkingStrategy {
    fun detectBoundaries(segments: List<String>, embeddings: List<FloatArray>): List<Int>
}

class BreakpointSemanticChunker(
    private val embeddingService: EmbeddingService,
    private val similarityThreshold: Double = 0.5,
    private val percentileBreakpoint: Int = 95
) : SemanticChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val sentences = splitIntoSentences(document.content)
        val embeddings = embeddingService.generateEmbeddings(sentences, modelConfig)
        val distances = computeAdjacentDistances(embeddings)
        val breakpoints = findBreakpoints(distances, percentileBreakpoint)
        return createChunksFromBreakpoints(sentences, breakpoints)
    }
}
```

### 2.2 Sliding Window with Context

**Concept**: Maintain context by including surrounding text in embeddings.

```kotlin
class SlidingWindowContextChunker(
    private val windowSize: Int = 3,
    private val contextSize: Int = 1
) : SemanticChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val sentences = splitIntoSentences(document.content)
        return sentences.windowed(windowSize, step = windowSize - contextSize)
            .mapIndexed { index, window ->
                DocumentChunk(
                    content = window.joinToString(" "),
                    index = index,
                    metadata = ChunkMetadata(
                        contextBefore = if (index > 0) sentences[index - 1] else null,
                        contextAfter = sentences.getOrNull(index + windowSize)
                    )
                )
            }
    }
}
```

### 2.3 Topic-Aware Chunking

**Concept**: Detect topic shifts using LDA or embedding clustering.

```kotlin
class TopicAwareChunker(
    private val embeddingService: EmbeddingService,
    private val clusteringService: ClusteringService,
    private val minTopicSize: Int = 3
) : SemanticChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val paragraphs = splitIntoParagraphs(document.content)
        val embeddings = embeddingService.generateEmbeddings(paragraphs, modelConfig)
        val topicAssignments = clusteringService.cluster(embeddings)
        return groupByTopic(paragraphs, topicAssignments)
    }
}
```

---

## Phase 3: Composite Multi-Strategy Chunker

### 3.1 Strategy Composition

**Concept**: Combine multiple strategies with weighted voting.

```kotlin
class CompositeSemanticChunker(
    private val strategies: List<WeightedStrategy>,
    private val boundaryMerger: BoundaryMerger
) : ChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val boundaryVotes = strategies.map { (strategy, weight) ->
            val boundaries = strategy.detectBoundaries(document)
            boundaries.map { it to weight }
        }.flatten()

        val mergedBoundaries = boundaryMerger.merge(boundaryVotes)
        return createChunks(document, mergedBoundaries)
    }
}

data class WeightedStrategy(
    val strategy: SemanticChunkingStrategy,
    val weight: Double
)
```

### 3.2 Content-Type Router

```kotlin
class ContentTypeRouter(
    private val strategyRegistry: Map<String, ChunkingStrategy>,
    private val defaultStrategy: ChunkingStrategy
) : ChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val strategy = strategyRegistry[document.mimeType] ?: defaultStrategy
        return strategy.chunk(document)
    }
}
```

**Registry Configuration**:
| MIME Type | Strategy |
|-----------|----------|
| `text/markdown` | `MarkdownChunkingStrategy` |
| `text/html` | `HtmlChunkingStrategy` |
| `application/pdf` | `SemanticChunkingStrategy` |
| `text/x-python`, `text/javascript` | `CodeAwareChunkingStrategy` |
| `application/json` | `JsonChunkingStrategy` |
| Default | `CompositeSemanticChunker` |

---

## Phase 4: Hierarchical and Parent-Child Chunking

### 4.1 Hierarchical Document Representation

```kotlin
class HierarchicalChunker(
    private val levels: List<ChunkLevel>
) : ChunkingStrategy {

    data class ChunkLevel(
        val name: String,
        val targetSize: Int,
        val strategy: ChunkingStrategy
    )

    override fun chunk(document: Document): List<DocumentChunk> {
        val hierarchy = mutableMapOf<String, HierarchyNode>()

        levels.forEachIndexed { levelIndex, level ->
            val chunks = level.strategy.chunk(document)
            chunks.forEach { chunk ->
                val node = HierarchyNode(chunk, levelIndex)
                if (levelIndex > 0) {
                    node.parent = findParent(hierarchy, chunk)
                }
                hierarchy[chunk.id] = node
            }
        }

        return flattenWithHierarchy(hierarchy)
    }
}
```

### 4.2 Parent Document Retriever Integration

```kotlin
class ParentDocumentChunker(
    private val parentChunkSize: Int = 2000,
    private val childChunkSize: Int = 400,
    private val childStrategy: ChunkingStrategy
) : ChunkingStrategy {

    override fun chunk(document: Document): List<DocumentChunk> {
        val parentChunks = FixedSizeChunkingStrategy(parentChunkSize).chunk(document)

        return parentChunks.flatMap { parent ->
            val children = childStrategy.chunk(Document(parent.content, document.mimeType))
            children.map { child ->
                child.copy(
                    parentChunkId = parent.id,
                    metadata = child.metadata.copy(parentContent = parent.content)
                )
            }
        }
    }
}
```

---

## Phase 5: Quality Metrics Framework

### 5.1 Chunk Quality Metrics

```kotlin
interface ChunkQualityMetric {
    val name: String
    fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>): MetricResult
}

data class MetricResult(
    val score: Double,
    val details: Map<String, Any>
)

class InternalCoherenceMetric(
    private val embeddingService: EmbeddingService
) : ChunkQualityMetric {

    override val name = "internal_coherence"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>): MetricResult {
        val coherenceScores = chunks.mapIndexed { i, chunk ->
            val sentenceEmbeddings = getSentenceEmbeddings(chunk)
            computeAverageSimilarity(sentenceEmbeddings)
        }
        return MetricResult(
            score = coherenceScores.average(),
            details = mapOf("per_chunk_scores" to coherenceScores)
        )
    }
}

class BoundaryQualityMetric : ChunkQualityMetric {
    override val name = "boundary_quality"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>): MetricResult {
        val betweenChunkDistances = embeddings.zipWithNext { a, b ->
            1 - cosineSimilarity(a, b)
        }
        return MetricResult(
            score = betweenChunkDistances.average(),
            details = mapOf("distances" to betweenChunkDistances)
        )
    }
}

class SizeDistributionMetric(
    private val targetSize: Int,
    private val tolerance: Double = 0.3
) : ChunkQualityMetric {
    override val name = "size_distribution"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>): MetricResult {
        val sizes = chunks.map { it.content.length }
        val withinTolerance = sizes.count {
            it >= targetSize * (1 - tolerance) && it <= targetSize * (1 + tolerance)
        }
        return MetricResult(
            score = withinTolerance.toDouble() / sizes.size,
            details = mapOf(
                "mean_size" to sizes.average(),
                "std_dev" to sizes.standardDeviation(),
                "min" to sizes.min(),
                "max" to sizes.max()
            )
        )
    }
}
```

### 5.2 Composite Quality Evaluator

```kotlin
class ChunkQualityEvaluator(
    private val metrics: List<ChunkQualityMetric>,
    private val weights: Map<String, Double>
) {
    fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>): QualityReport {
        val results = metrics.map { metric ->
            metric.name to metric.evaluate(chunks, embeddings)
        }.toMap()

        val overallScore = results.entries.sumOf { (name, result) ->
            result.score * (weights[name] ?: 1.0)
        } / weights.values.sum()

        return QualityReport(
            overallScore = overallScore,
            metricResults = results,
            recommendations = generateRecommendations(results)
        )
    }
}
```

---

## Phase 6: Strategy Weight Optimization

### 6.1 Bayesian Optimization for Strategy Weights

```kotlin
class StrategyWeightOptimizer(
    private val strategies: List<SemanticChunkingStrategy>,
    private val qualityEvaluator: ChunkQualityEvaluator,
    private val evaluationCorpus: List<Document>
) {
    fun optimize(iterations: Int = 50): Map<String, Double> {
        val optimizer = BayesianOptimizer(
            dimensions = strategies.size,
            acquisitionFunction = ExpectedImprovement()
        )

        repeat(iterations) {
            val weights = optimizer.suggest()
            val score = evaluateWithWeights(weights)
            optimizer.observe(weights, score)
        }

        return optimizer.bestParameters().mapIndexed { i, weight ->
            strategies[i].name to weight
        }.toMap()
    }

    private fun evaluateWithWeights(weights: List<Double>): Double {
        val chunker = CompositeSemanticChunker(
            strategies.zip(weights).map { (s, w) -> WeightedStrategy(s, w) }
        )

        return evaluationCorpus.map { doc ->
            val chunks = chunker.chunk(doc)
            val embeddings = embeddingService.generateEmbeddings(chunks.map { it.content })
            qualityEvaluator.evaluate(chunks, embeddings).overallScore
        }.average()
    }
}
```

### 6.2 A/B Testing Framework

```kotlin
class ChunkerABTester(
    private val embeddingGateway: EmbeddingGateway,
    private val metricsCollector: MetricsCollector
) {
    data class ABTestConfig(
        val testId: String,
        val controlChunker: ChunkingStrategy,
        val treatmentChunker: ChunkingStrategy,
        val trafficSplit: Double = 0.5,
        val minimumSampleSize: Int = 100
    )

    fun runTest(config: ABTestConfig, documents: List<Document>): ABTestResult {
        val (control, treatment) = documents.partition { Random.nextDouble() < config.trafficSplit }

        val controlMetrics = evaluateChunker(config.controlChunker, control)
        val treatmentMetrics = evaluateChunker(config.treatmentChunker, treatment)

        return ABTestResult(
            testId = config.testId,
            controlMetrics = controlMetrics,
            treatmentMetrics = treatmentMetrics,
            statisticalSignificance = computeSignificance(controlMetrics, treatmentMetrics),
            recommendation = generateRecommendation(controlMetrics, treatmentMetrics)
        )
    }
}
```

---

## Phase 7: LLM-Assisted Boundary Detection

### 7.1 LLM Boundary Detector

```kotlin
class LLMBoundaryDetector(
    private val llmClient: LLMClient,
    private val batchSize: Int = 10
) : SemanticChunkingStrategy {

    private val prompt = """
        Analyze the following text and identify natural semantic boundaries.
        Mark boundary positions with [BOUNDARY] tags.
        Consider:
        - Topic transitions
        - Logical section breaks
        - Conceptual shifts
        - Narrative or argumentative structure

        Text:
        {{text}}

        Return the text with [BOUNDARY] markers inserted at appropriate break points.
    """.trimIndent()

    override fun detectBoundaries(document: Document): List<Int> {
        val paragraphs = document.content.split("\n\n")
        val annotatedParagraphs = paragraphs.chunked(batchSize).flatMap { batch ->
            llmClient.complete(prompt.replace("{{text}}", batch.joinToString("\n\n")))
                .split("[BOUNDARY]")
        }
        return extractBoundaryPositions(document.content, annotatedParagraphs)
    }
}
```

---

## Implementation Roadmap

### Sprint 1: Foundation (Weeks 1-2)
- [ ] Create chunking strategy interfaces in commons
- [ ] Implement `FixedSizeChunkingStrategy` (refactor existing)
- [ ] Implement `SentenceAwareChunkingStrategy`
- [ ] Implement `RecursiveCharacterSplitter`
- [ ] Add unit tests for all strategies

### Sprint 2: Content-Aware Strategies (Weeks 3-4)
- [ ] Implement `MarkdownChunkingStrategy`
- [ ] Implement `CodeAwareChunkingStrategy`
- [ ] Implement `HtmlChunkingStrategy`
- [ ] Create `ContentTypeRouter`
- [ ] Integration tests with real documents

### Sprint 3: Semantic Strategies (Weeks 5-6)
- [ ] Implement `BreakpointSemanticChunker`
- [ ] Implement `SlidingWindowContextChunker`
- [ ] Implement `TopicAwareChunker`
- [ ] Add clustering service dependency

### Sprint 4: Composite and Hierarchical (Weeks 7-8)
- [ ] Implement `CompositeSemanticChunker`
- [ ] Implement `HierarchicalChunker`
- [ ] Implement `ParentDocumentChunker`
- [ ] Update PgVector schema for hierarchy

### Sprint 5: Quality Framework (Weeks 9-10)
- [ ] Implement quality metrics
- [ ] Create `ChunkQualityEvaluator`
- [ ] Add metrics to processing pipeline
- [ ] Dashboard for quality monitoring

### Sprint 6: Optimization (Weeks 11-12)
- [ ] Implement weight optimizer
- [ ] Add A/B testing framework
- [ ] Create optimization jobs
- [ ] Documentation and tuning guide

---

## Configuration Schema

```yaml
ingestion:
  chunking:
    default-strategy: composite
    strategies:
      fixed-size:
        chunk-size: 1000
        overlap: 200
      sentence-aware:
        max-sentences: 5
        respect-paragraphs: true
      semantic:
        similarity-threshold: 0.5
        percentile-breakpoint: 95
        model: ${EMBEDDING_MODEL:nomic-embed-text}
      markdown:
        preserve-code-blocks: true
        preserve-tables: true
        heading-as-context: true
      hierarchical:
        levels:
          - name: document
            target-size: 4000
          - name: section
            target-size: 1000
          - name: paragraph
            target-size: 300

    content-type-mapping:
      text/markdown: markdown
      text/html: html
      application/pdf: semantic
      text/plain: composite
      text/x-python: code
      application/javascript: code

    composite:
      strategies:
        - name: sentence-aware
          weight: 0.3
        - name: semantic
          weight: 0.5
        - name: structural
          weight: 0.2

    quality:
      enabled: true
      metrics:
        - internal-coherence
        - boundary-quality
        - size-distribution
      minimum-score: 0.7

    optimization:
      enabled: false
      corpus-path: /data/optimization-corpus
      iterations: 50
```

---

## Database Schema Updates

### New Tables for Hierarchical Chunks

```sql
CREATE TABLE IF NOT EXISTS ingestion.chunk_hierarchy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id UUID NOT NULL,
    source_id UUID NOT NULL,
    chunk_id VARCHAR(255) NOT NULL,
    parent_chunk_id VARCHAR(255),
    level INT NOT NULL,
    level_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    FOREIGN KEY (source_id) REFERENCES ingestion.knowledge_sources(id)
);

CREATE INDEX idx_chunk_hierarchy_kb ON ingestion.chunk_hierarchy(knowledge_base_id);
CREATE INDEX idx_chunk_hierarchy_parent ON ingestion.chunk_hierarchy(parent_chunk_id);
```

### Quality Metrics Storage

```sql
CREATE TABLE IF NOT EXISTS ingestion.chunk_quality_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id UUID NOT NULL,
    source_id UUID NOT NULL,
    strategy_name VARCHAR(100) NOT NULL,
    overall_score DOUBLE PRECISION NOT NULL,
    metric_details JSONB NOT NULL,
    evaluated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_quality_metrics_kb ON ingestion.chunk_quality_metrics(knowledge_base_id);
```

---

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Retrieval Accuracy (MRR@10) | Baseline | +20% |
| Chunk Coherence Score | N/A | > 0.8 |
| Boundary Quality Score | N/A | > 0.7 |
| Processing Latency | Baseline | < 2x increase |
| User Satisfaction (answer quality) | Baseline | +15% |

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Increased processing time | Medium | Async processing, caching, batch optimization |
| Embedding API costs | High | Local models (Ollama), caching, smart sampling |
| Complexity overhead | Medium | Feature flags, gradual rollout, monitoring |
| Breaking existing behavior | High | A/B testing, rollback capability |

---

## Next Steps

1. **Review and Approve**: Stakeholder review of this plan
2. **Prioritize**: Identify must-have vs nice-to-have features
3. **Prototype**: Build PoC for semantic chunking
4. **Measure**: Establish baseline metrics before changes
5. **Iterate**: Implement in sprints with continuous feedback
