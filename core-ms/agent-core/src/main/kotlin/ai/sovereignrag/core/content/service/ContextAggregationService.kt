package ai.sovereignrag.core.content.service

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.model.embedding.EmbeddingModel
import mu.KotlinLogging
import nl.compilot.ai.content.event.ContentIngestionEvent
import nl.compilot.ai.content.store.TenantAwarePgVectorStoreFactory
import nl.compilot.ai.domain.ContentDocument
import nl.compilot.ai.commons.tenant.TenantContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Service to aggregate content and generate contextual documents
 * Triggered by content ingestion events
 */
@Service
class ContextAggregationService(
    private val contentService: ContentService,
    private val chatLanguageModel: ChatLanguageModel,
    private val embeddingModel: EmbeddingModel,
    private val pgVectorStoreFactory: TenantAwarePgVectorStoreFactory
) {

    /**
     * Generate site-wide overview document
     * Analyzes all content to create a comprehensive site profile
     */
    fun generateSiteProfile() {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Generating site profile" }

        try {
            val metadata = queryAllDocumentsMetadata()

            if (metadata.isEmpty()) {
                logger.warn { "[$tenantId] No documents found, skipping site profile generation" }
                return
            }

            val categories = metadata.mapNotNull { it["category"] }.distinct()
            val titles = metadata.mapNotNull { it["title"] }.take(20)
            val siteTitle = metadata.firstNotNullOfOrNull { it["site_title"] } ?: "Website"
            val siteTagline = metadata.firstNotNullOfOrNull { it["site_tagline"] } ?: ""

            val prompt = """
Analyze this website and create a comprehensive overview:

Site Name: $siteTitle
Tagline: $siteTagline
Total Documents: ${metadata.size}
Categories: ${categories.joinToString(", ")}
Sample Content: ${titles.joinToString(", ")}

Generate a clear, concise description covering:
1. What this website is about (2-3 sentences)
2. Main topics or categories covered
3. Target audience or purpose
4. Key services, products, or information offered

Be specific and informative. Write in a helpful, professional tone.
            """.trimIndent()

            val siteProfile = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            logger.info { "[$tenantId] Generated site profile (${siteProfile.length} chars)" }

            // Ingest as special document type
            val contentDoc = ContentDocument(
                id = "site-profile-$tenantId",
                title = "About $siteTitle",
                content = """
# About $siteTitle

$siteProfile

---
*This is an automatically generated overview of the website content. It is updated periodically as new content is added.*
                """.trimIndent(),
                source = "system-generated",
                createdAt = LocalDateTime.now(),
                metadata = mapOf(
                    "post_type" to "site_profile",
                    "priority" to "high",
                    "generated_at" to LocalDateTime.now().toString(),
                    "document_count" to metadata.size.toString()
                )
            )

            contentService.ingest(contentDoc)
            logger.info { "[$tenantId] Site profile ingested successfully" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to generate site profile" }
        }
    }

    /**
     * Incrementally update site profile with new content
     * Updates existing profile instead of regenerating from scratch
     */
    fun incrementallyUpdateSiteProfile(event: ContentIngestionEvent) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Incrementally updating site profile with: ${event.title}" }

        try {
            // 1. Try to retrieve existing site profile
            val existingProfile = getSiteProfile()

            // 2. If no profile exists, generate from scratch
            if (existingProfile == null) {
                logger.info { "[$tenantId] No existing site profile found, generating from scratch" }
                generateSiteProfile()
                return
            }

            logger.info { "[$tenantId] Found existing site profile, performing incremental update" }

            // 3. Get site metadata for context
            val metadata = queryAllDocumentsMetadata()
            val siteTitle = metadata.firstNotNullOfOrNull { it["site_title"] } ?: "Website"

            // 4. Incremental update prompt
            val prompt = """
Current site overview:
$existingProfile

New content just added:
- Title: ${event.title}
- Category: ${event.category ?: "Uncategorized"}
- Type: ${event.postType}
- Total documents now: ${metadata.size}

Update the site overview to incorporate this new information if relevant.
Consider:
1. Does this reveal new services/products?
2. Does this cover new topics not mentioned before?
3. Does this provide insights about target audience?
4. Does this change our understanding of the site's purpose?

IMPORTANT:
- Only update what's necessary
- Maintain the same structure and tone
- If nothing significant changes, return the current overview with minimal adjustments
- Keep it concise (2-3 sentences per section)
- Be specific and informative

Generate the updated overview:
            """.trimIndent()

            val updatedProfile = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            logger.info { "[$tenantId] Generated updated profile (${updatedProfile.length} chars)" }

            // 5. Save updated profile (reuse existing document ID)
            val contentDoc = ContentDocument(
                id = "site-profile-$tenantId",
                title = "About $siteTitle",
                content = """
# About $siteTitle

$updatedProfile

---
*This is an automatically generated overview of the website content. It is updated with each new content addition.*
                """.trimIndent(),
                source = "system-generated",
                createdAt = LocalDateTime.now(),
                metadata = mapOf(
                    "post_type" to "site_profile",
                    "priority" to "high",
                    "updated_at" to LocalDateTime.now().toString(),
                    "document_count" to metadata.size.toString(),
                    "last_update_trigger" to event.title
                )
            )

            contentService.ingest(contentDoc)
            logger.info { "[$tenantId] Site profile incrementally updated successfully" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to incrementally update site profile" }
        }
    }

    /**
     * Retrieve existing site profile content
     */
    private fun getSiteProfile(): String? {
        val tenantId = TenantContext.getCurrentTenant()

        return try {
            val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()

            // Search for site profile document
            val embedding = embeddingModel.embed("site profile overview").content()

            val searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(50)
                .minScore(0.0)
                .build()

            val results = embeddingStore.search(searchRequest)

            // Find the site profile document
            val profileMatch = results.matches().find { match ->
                match.embedded()?.metadata()?.get("post_type")?.toString() == "site_profile"
            }

            val content = profileMatch?.embedded()?.text()

            if (content != null) {
                logger.info { "[$tenantId] Retrieved existing site profile (${content.length} chars)" }
                // Extract just the profile content (without markdown header and footer)
                content.substringAfter("# About")
                    .substringBefore("---")
                    .trim()
                    .removePrefix(content.substringAfter("# About").substringBefore("\n"))
                    .trim()
            } else {
                logger.info { "[$tenantId] No existing site profile found" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to retrieve site profile" }
            null
        }
    }

    /**
     * Generate category summary
     * Creates an overview document for a specific category
     */
    fun generateCategorySummary(category: String) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Generating summary for category: $category" }

        try {
            val documents = queryDocumentsByCategory(category)

            if (documents.isEmpty()) {
                logger.warn { "[$tenantId] No documents found in category: $category" }
                return
            }

            val titles = documents.mapNotNull { it["title"] }.take(10)
            val excerpts = documents.mapNotNull { it["excerpt"] }.take(5)
            val categoryDesc = documents.firstNotNullOfOrNull { it["category_description"] }

            val prompt = """
Summarize the content from the "$category" category:

Category Description: ${categoryDesc ?: "Not provided"}
Number of Documents: ${documents.size}
Document Titles: ${titles.joinToString("\n- ", prefix = "\n- ")}
${if (excerpts.isNotEmpty()) "Sample Content:\n${excerpts.joinToString("\n\n")}" else ""}

Create a comprehensive overview that explains:
1. What this category covers
2. Main topics and themes
3. Key takeaways or important information
4. Who would benefit from this content

Be informative and specific.
            """.trimIndent()

            val summary = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            logger.info { "[$tenantId] Generated category summary for: $category (${summary.length} chars)" }

            // Ingest as category summary
            val contentDoc = ContentDocument(
                id = "category-$category-$tenantId",
                title = "About: $category",
                content = """
# $category Overview

$summary

---
*This summary covers ${documents.size} documents in the $category category.*
                """.trimIndent(),
                source = "system-generated",
                createdAt = LocalDateTime.now(),
                metadata = mapOf(
                    "post_type" to "category_summary",
                    "category" to category,
                    "document_count" to documents.size.toString(),
                    "generated_at" to LocalDateTime.now().toString()
                )
            )

            contentService.ingest(contentDoc)
            logger.info { "[$tenantId] Category summary ingested: $category" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to generate category summary for: $category" }
        }
    }

    /**
     * Generate synthetic FAQs from content
     */
    fun generateCommonQuestions() {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Generating common questions" }

        try {
            val recentDocs = queryAllDocumentsMetadata().take(50)

            if (recentDocs.size < 5) {
                logger.warn { "[$tenantId] Not enough documents to generate FAQs (${recentDocs.size})" }
                return
            }

            val titles = recentDocs.mapNotNull { it["title"] }
            val categories = recentDocs.mapNotNull { it["category"] }.distinct()
            val siteTitle = recentDocs.firstNotNullOfOrNull { it["site_title"] } ?: "this website"

            val prompt = """
Based on this website's content, generate 8 common questions a visitor might ask:

Website: $siteTitle
Categories: ${categories.joinToString(", ")}
Content includes: ${titles.take(15).joinToString(", ")}

For each question, provide a concise answer (2-3 sentences) based on the content topics.

IMPORTANT: Include general questions like:
- "What is this website about?"
- "What services/products do you offer?"
- "Who is this for?"
- "How can I get help?"

Format as:
Q: [Question]
A: [Answer]

Q: [Question]
A: [Answer]
            """.trimIndent()

            val response = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            // Parse Q&A pairs
            val qaPattern = Regex("""Q:\s*(.+?)\s*A:\s*(.+?)(?=Q:|$)""", RegexOption.DOT_MATCHES_ALL)
            val matches = qaPattern.findAll(response)

            matches.forEach { match ->
                val question = match.groupValues[1].trim()
                val answer = match.groupValues[2].trim()

                if (question.isNotBlank() && answer.isNotBlank()) {
                    val contentDoc = ContentDocument(
                        id = "faq-${UUID.randomUUID()}",
                        title = question,
                        content = """
# $question

$answer
                        """.trimIndent(),
                        source = "system-generated",
                        createdAt = LocalDateTime.now(),
                        metadata = mapOf(
                            "post_type" to "synthetic_faq",
                            "question_type" to "general",
                            "generated_at" to LocalDateTime.now().toString()
                        )
                    )

                    contentService.ingest(contentDoc)
                }
            }

            logger.info { "[$tenantId] Generated and ingested FAQs" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to generate common questions" }
        }
    }

    // Helper methods

    private fun queryAllDocumentsMetadata(): List<Map<String, String>> {
        val tenantId = TenantContext.getCurrentTenant()

        return try {
            val embeddingStore = pgVectorStoreFactory.getEmbeddingStore()

            // Use a generic embedding to retrieve all documents
            val dummyEmbedding = embeddingModel.embed("overview").content()

            val searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .maxResults(1000)
                .minScore(0.0)
                .build()

            val results = embeddingStore.search(searchRequest)

            results.matches().mapNotNull { match ->
                match.embedded()?.metadata()?.toMap()?.mapValues { it.value.toString() }
            }
        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to query documents metadata" }
            emptyList()
        }
    }

    private fun queryDocumentsByCategory(category: String): List<Map<String, String>> {
        return queryAllDocumentsMetadata().filter { it["category"] == category }
    }

    /**
     * Extract and embed entity relationships from a document
     * Creates synthetic documents for important entities (products, people, locations, concepts)
     */
    fun extractAndEmbedEntities(contentDoc: ContentDocument) {
        val tenantId = TenantContext.getCurrentTenant()
        logger.info { "[$tenantId] Extracting entities from: ${contentDoc.title}" }

        try {
            val prompt = """
Extract key entities from this content. Focus on:
- Products or services mentioned
- People or organizations
- Locations
- Important concepts or terms

Title: ${contentDoc.title}
Content: ${contentDoc.content.take(2000)}

For each entity, provide:
1. Entity name
2. Type (product/person/location/concept)
3. Brief description (1-2 sentences)

Format as:
Entity: [name]
Type: [type]
Description: [description]

---
            """.trimIndent()

            val response = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            // Parse entities
            val entityBlocks = response.split("---").filter { it.isNotBlank() }

            entityBlocks.forEach { block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }

                val entityName = lines.find { it.startsWith("Entity:") }
                    ?.substringAfter("Entity:")?.trim()

                val entityType = lines.find { it.startsWith("Type:") }
                    ?.substringAfter("Type:")?.trim()

                val entityDesc = lines.find { it.startsWith("Description:") }
                    ?.substringAfter("Description:")?.trim()

                if (!entityName.isNullOrBlank() && !entityDesc.isNullOrBlank()) {
                    // Create entity document
                    val entityDoc = ContentDocument(
                        id = "entity-${UUID.randomUUID()}",
                        title = entityName,
                        content = """
# $entityName

Type: ${entityType ?: "General"}

$entityDesc

Related to: ${contentDoc.title}
Source: ${contentDoc.url}
                        """.trimIndent(),
                        source = "system-generated",
                        createdAt = LocalDateTime.now(),
                        metadata = mapOf(
                            "post_type" to "entity",
                            "entity_type" to (entityType ?: "general"),
                            "entity_name" to entityName,
                            "source_document" to contentDoc.title,
                            "source_url" to (contentDoc.url ?: ""),
                            "generated_at" to LocalDateTime.now().toString()
                        )
                    )

                    contentService.ingest(entityDoc)
                    logger.debug { "[$tenantId] Created entity: $entityName ($entityType)" }
                }
            }

            logger.info { "[$tenantId] Entity extraction complete for: ${contentDoc.title}" }

        } catch (e: Exception) {
            logger.error(e) { "[$tenantId] Failed to extract entities from: ${contentDoc.title}" }
        }
    }
}
