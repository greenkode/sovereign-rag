# ConversationalAgentService Refactoring Analysis

## Current Method: `processChatInteraction()`

### Cyclomatic Complexity Issues
- **~485 lines** in single method
- **Multiple nested if/else** branches (7+ decision points)
- **Duplicated code** across branches (confidence extraction, language prefixes, etc.)
- **High coupling** - method knows about KB search, general knowledge, conversation history, sources, identity detection, etc.

---

## Core Functionality Breakdown

### 1. **Pre-Processing Phase**
- Detect/determine language from message or session
- Detect if query is an identity question
- Perform parallel KB search + language instruction generation

### 2. **Decision Tree: Response Strategy Selection**
Based on these conditions:
- `hasHighQualityResults` (KB confidence >= threshold)
- `useGeneralKnowledge` (WordPress setting)
- `isIdentityQuery` (detected identity question)
- `hasConversationHistory` (can answer from chat history)

Results in **5 distinct response strategies**:

```
├─ KB_HIGH_QUALITY (hasHighQualityResults == true)
│   ├─ Build context from high-confidence KB results
│   ├─ Apply KB-only restriction if general knowledge disabled
│   └─ Return KB-based answer
│
├─ GENERAL_KNOWLEDGE_WITH_CONTEXT (hasHighQualityResults == false && useGeneralKnowledge == true)
│   ├─ IDENTITY_WITH_CONTEXT (isIdentityQuery == true && context != null)
│   ├─ IDENTITY_NO_CONTEXT (isIdentityQuery == true && context == null)
│   ├─ GENERAL_WITH_CONTEXT (isIdentityQuery == false && context != null)
│   └─ GENERAL_NO_CONTEXT (isIdentityQuery == false && context == null)
│
└─ KB_ONLY_FALLBACK (hasHighQualityResults == false && useGeneralKnowledge == false)
    ├─ CONVERSATION_HISTORY (hasConversationHistory == true)
    └─ NO_RESULTS (hasConversationHistory == false)
```

### 3. **Cross-Cutting Concerns** (Applied to All Strategies)
- **Language Handling**: Add language instruction to prompts
- **Source Citation**: 3 variations (include, user disabled, not available)
- **Confidence Extraction**: Parse `[CONFIDENCE: XX%]` from response
- **Escalation Detection**: Check for `__ESCALATION_READY__` keyword
- **Unanswered Query Logging**: Track queries without good answers

---

## Proposed Architecture: Strategy Pattern

### Strategy Interface

```kotlin
/**
 * Strategy for handling different types of chat interactions
 */
interface ChatResponseStrategy {
    /**
     * Check if this strategy can handle the given context
     */
    fun canHandle(context: ChatContext): Boolean

    /**
     * Priority for strategy selection (higher = checked first)
     */
    fun priority(): Int = 0

    /**
     * Generate response using this strategy
     */
    fun generateResponse(context: ChatContext): ChatResponse
}
```

### Context Object (Encapsulates All Inputs)

```kotlin
data class ChatContext(
    val session: ChatSession,
    val message: String,
    val effectiveLanguage: String?,
    val isIdentityQuery: Boolean,
    val searchResults: List<SearchResult>,
    val hasHighQualityResults: Boolean,
    val useGeneralKnowledge: Boolean,
    val showGkDisclaimer: Boolean,
    val gkDisclaimerText: String?,
    val showSources: Boolean,
    val properties: SovereignRagProperties
)
```

### Strategy Implementations

```kotlin
@Component
@Order(1) // Highest priority
class KnowledgeBaseStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) = context.hasHighQualityResults
    override fun priority() = 100

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 335-505
        // Build KB context, apply restrictions, generate response
    }
}

@Component
@Order(2)
class IdentityWithContextStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) =
        !context.hasHighQualityResults &&
        context.useGeneralKnowledge &&
        context.isIdentityQuery &&
        context.searchResults.isNotEmpty()

    override fun priority() = 90

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 554-577
    }
}

@Component
@Order(3)
class IdentityNoContextStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) =
        !context.hasHighQualityResults &&
        context.useGeneralKnowledge &&
        context.isIdentityQuery &&
        context.searchResults.isEmpty()

    override fun priority() = 85

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 578-592
    }
}

@Component
@Order(4)
class GeneralKnowledgeWithContextStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) =
        !context.hasHighQualityResults &&
        context.useGeneralKnowledge &&
        !context.isIdentityQuery &&
        context.searchResults.isNotEmpty()

    override fun priority() = 80

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 593-615
    }
}

@Component
@Order(5)
class GeneralKnowledgeNoContextStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) =
        !context.hasHighQualityResults &&
        context.useGeneralKnowledge &&
        !context.isIdentityQuery &&
        context.searchResults.isEmpty()

    override fun priority() = 75

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 616-634
    }
}

@Component
@Order(6)
class ConversationHistoryStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) =
        !context.hasHighQualityResults &&
        !context.useGeneralKnowledge &&
        context.session.chatMemory.messages().isNotEmpty()

    override fun priority() = 50

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 675-700
    }
}

@Component
@Order(7) // Lowest priority - fallback
class NoResultsStrategy : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) = true // Always can handle (fallback)
    override fun priority() = 0

    override fun generateResponse(context: ChatContext): ChatResponse {
        // Current lines 714-723
    }
}
```

### Orchestrator (Replaces processChatInteraction)

```kotlin
@Service
class ChatResponseOrchestrator(
    private val strategies: List<ChatResponseStrategy>,
    private val promptInstructionService: PromptInstructionService, // NEW
    private val confidenceExtractor: ConfidenceExtractor, // NEW
    private val escalationDetector: EscalationDetector // NEW
) {

    fun processChatInteraction(
        session: ChatSession,
        message: String,
        useGeneralKnowledge: Boolean = true,
        showGkDisclaimer: Boolean = false,
        gkDisclaimerText: String? = null,
        showSources: Boolean = true
    ): ChatInteractionResult {

        // 1. Pre-processing
        val effectiveLanguage = determineLanguage(session, message)
        val isIdentityQuery = isIdentityQuestion(message)

        val (languageInstruction, searchResults) = performParallelSearch(
            message, effectiveLanguage
        )

        val hasHighQualityResults = checkHighQualityResults(searchResults)

        // 2. Build context
        val context = ChatContext(
            session, message, effectiveLanguage, isIdentityQuery,
            searchResults, hasHighQualityResults, useGeneralKnowledge,
            showGkDisclaimer, gkDisclaimerText, showSources, properties
        )

        // 3. Select and execute strategy
        val strategy = strategies
            .sortedByDescending { it.priority() }
            .first { it.canHandle(context) }

        logger.info { "Using strategy: ${strategy::class.simpleName}" }

        val response = strategy.generateResponse(context)

        // 4. Post-processing (cross-cutting concerns)
        val processedResponse = postProcess(response, context)

        return processedResponse.toResult()
    }

    private fun postProcess(
        response: ChatResponse,
        context: ChatContext
    ): ProcessedChatResponse {
        // Extract confidence
        val (finalResponse, confidence) = confidenceExtractor.extract(response.text)

        // Detect escalation
        val escalationInfo = escalationDetector.detect(finalResponse, context.session)

        // Log unanswered queries
        logIfUnanswered(context, response, confidence)

        return ProcessedChatResponse(finalResponse, response.sources, confidence, escalationInfo)
    }
}
```

---

## Benefits of Refactoring

### 1. **Separation of Concerns**
- Each strategy handles ONE specific case
- Cross-cutting concerns extracted to dedicated services
- Orchestrator only coordinates, doesn't implement logic

### 2. **Testability**
- Each strategy can be unit tested independently
- Mock strategies for integration testing
- Test cross-cutting concerns separately

### 3. **Extensibility**
- Add new strategy by implementing interface + `@Component`
- No need to modify existing code (Open/Closed Principle)
- Easy to add A/B testing for strategies

### 4. **Maintainability**
- Each class < 100 lines vs 485-line method
- Clear naming shows what each strategy does
- Easier to understand flow

### 5. **Configurability**
- Strategy priority can be database-driven
- Can enable/disable strategies via feature flags
- Tenant-specific strategy customization

---

## Additional Services to Extract

### PromptInstructionService
Centralize all instruction loading from templates:
```kotlin
@Service
class PromptInstructionService(
    private val promptTemplateService: PromptTemplateService
) {
    fun getSourceInstructions(showSources: Boolean, hasActualSources: Boolean): String
    fun getConfidenceInstruction(needsConfidence: Boolean): String
    fun getLanguagePrefix(language: String?): String
    fun getRestrictionInstruction(useGeneralKnowledge: Boolean): String
    fun getIdentityInstructions(hasContext: Boolean, showSources: Boolean, sources: List<String>): String
    fun getGeneralKnowledgeInstructions(hasContext: Boolean, showSources: Boolean, sources: List<String>, disclaimerText: String?): String
}
```

### ConfidenceExtractor
```kotlin
@Service
class ConfidenceExtractor {
    fun extract(response: String): Pair<String, Int?> {
        val regex = """\[CONFIDENCE:\s*(\d+)%?\]""".toRegex()
        val match = regex.find(response)
        val confidence = match?.groupValues?.get(1)?.toIntOrNull()
        val cleaned = response.replace(regex, "").trim()
        return Pair(cleaned, confidence)
    }
}
```

### EscalationDetector
```kotlin
@Service
class EscalationDetector(private val emailTool: EmailTool) {
    fun detect(response: String, session: ChatSession): EscalationInfo? {
        if (!response.contains("__ESCALATION_READY__")) return null
        return extractEscalationInfo(session)
    }
}
```

---

## Migration Path

### Phase 1: Extract Services
1. Create `PromptInstructionService` with all template loading
2. Create `ConfidenceExtractor`
3. Create `EscalationDetector`
4. Refactor `processChatInteraction` to use these services (reduce to ~300 lines)

### Phase 2: Define Interfaces
1. Create `ChatResponseStrategy` interface
2. Create `ChatContext` data class
3. Create `ChatResponse` data class

### Phase 3: Extract Strategies (One at a Time)
1. Start with simplest: `NoResultsStrategy`
2. Then `ConversationHistoryStrategy`
3. Then `KnowledgeBaseStrategy`
4. Finally all general knowledge variations
5. Test each migration step

### Phase 4: Create Orchestrator
1. Implement `ChatResponseOrchestrator`
2. Wire strategies via Spring dependency injection
3. Switch `ConversationalAgentService` to delegate to orchestrator
4. Remove old `processChatInteraction` implementation

### Phase 5: Cleanup
1. Remove unused helper methods
2. Update tests
3. Add strategy-level tests

---

## Example: Before vs After

### Before (485 lines, cyclomatic complexity ~15)
```kotlin
fun processChatInteraction(...): ChatInteractionResult {
    if (hasHighQualityResults) {
        if (!useGeneralKnowledge) {
            // KB-only restriction
        }
        if (showSources && hasActualSources) {
            // Include sources
        } else if (!showSources) {
            // User disabled
        } else {
            // No sources available
        }
        // ... 170 more lines
    } else if (useGeneralKnowledge) {
        if (isIdentityQuery) {
            if (!context.isNullOrBlank()) {
                // Identity with context
            } else {
                // Identity no context
            }
        } else if (!context.isNullOrBlank()) {
            // General with context
        } else {
            // General no context
        }
        // ... 130 more lines
    } else {
        if (hasConversationHistory) {
            // Try conversation history
        } else {
            // No results
        }
    }
    // ... post-processing
}
```

### After (orchestrator ~80 lines, each strategy 50-70 lines)
```kotlin
fun processChatInteraction(...): ChatInteractionResult {
    val context = buildContext(session, message, ...)
    val strategy = selectStrategy(context)
    val response = strategy.generateResponse(context)
    return postProcess(response, context)
}
```

Each strategy:
```kotlin
@Component
class KnowledgeBaseStrategy(...) : ChatResponseStrategy {
    override fun canHandle(context: ChatContext) = context.hasHighQualityResults
    override fun generateResponse(context: ChatContext): ChatResponse {
        val systemPrompt = buildSystemPrompt(context)
        val userPrompt = buildUserPrompt(context)
        return invokeModel(systemPrompt, userPrompt, context)
    }
}
```

---

## Metrics Improvement Estimate

| Metric | Before | After |
|--------|--------|-------|
| Largest method | 485 lines | 80 lines (orchestrator) |
| Cyclomatic complexity | ~15 | ~3 (orchestrator), ~2-4 (strategies) |
| Testability | Low (mock everything) | High (test strategies independently) |
| Adding new strategy | Modify 485-line method | Add new 50-line class |
| Code duplication | High | Low (extracted to services) |
