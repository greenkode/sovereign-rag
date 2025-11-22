# Incremental Context Updates Implementation

## Overview

Implemented incremental site profile updates that trigger on **EVERY content change** instead of milestone-based updates (1st, 10th, 25th documents). This provides a continuously updated aggregate context that grows with the knowledge base while remaining computationally efficient.

## Changes Made

### 1. ContextAggregationService.kt

Added two new methods:

#### `incrementallyUpdateSiteProfile(event: ContentIngestionEvent)`
- Triggers on every content ingestion event
- Retrieves existing site profile (if available)
- Falls back to full generation if no profile exists
- Creates an incremental update prompt that includes:
  - Current site overview
  - New content metadata (title, category, type)
  - Total document count
- Instructs LLM to only update what's necessary
- Maintains same structure and tone as existing profile
- Tracks update metadata (last_update_trigger, document_count, updated_at)

#### `getSiteProfile(): String?`
- Private helper method to retrieve existing site profile
- Searches pgvector embeddings store for documents with `post_type: "site_profile"`
- Extracts the profile content (without markdown formatting)
- Returns null if no profile exists

### 2. ContextEnrichmentListener.kt

Modified the event listener to call incremental updates:

**Before:**
```kotlin
val documentCount = getDocumentCount(event.tenantId)
if (shouldRegenerateSiteProfile(documentCount)) {
    contextAggregationService.generateSiteProfile()
}
```

**After:**
```kotlin
logger.info { "[${event.tenantId}] Incrementally updating site profile" }
contextAggregationService.incrementallyUpdateSiteProfile(event)
```

## How It Works

### On First Content Ingestion
1. Event listener receives ContentIngestionEvent
2. Calls `incrementallyUpdateSiteProfile()`
3. No existing profile found
4. Falls back to `generateSiteProfile()` (full generation)
5. Creates initial site profile from all available metadata

### On Subsequent Ingestions
1. Event listener receives ContentIngestionEvent
2. Calls `incrementallyUpdateSiteProfile()`
3. Retrieves existing site profile from pgvector
4. Builds incremental update prompt with:
   - Current profile content
   - New document metadata
   - Contextual questions (new services? new topics? new audience insights?)
5. LLM updates profile with new information
6. Saves updated profile (reuses same document ID: `site-profile-{tenantId}`)

## Benefits

### 1. Always Up-to-Date Context
- Site profile updates with every content change
- No stale information waiting for milestone thresholds
- Immediate reflection of new content in general knowledge queries

### 2. Computational Efficiency
- Incremental updates are faster than full regeneration
- LLM only processes: existing profile + new content metadata
- Doesn't need to re-analyze all documents

### 3. Scales Well with Local LLMs
- Small, focused prompts work well with Ollama
- Low computational cost per update
- Suitable for frequent updates

### 4. Maintains Context Quality
- Preserves structure and tone of existing profile
- Only updates relevant sections
- Avoids redundant regeneration when nothing significant changes

## Example Flow

### Content Ingestion Event
```kotlin
ContentIngestionEvent(
    tenantId = "dev",
    documentId = UUID.randomUUID(),
    title = "New Product Launch: AI-Powered Analytics",
    category = "Products",
    postType = "post",
    timestamp = Instant.now()
)
```

### Incremental Update Prompt
```
Current site overview:
This website is a technology company specializing in business intelligence solutions...

New content just added:
- Title: New Product Launch: AI-Powered Analytics
- Category: Products
- Type: post
- Total documents now: 47

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
```

### Updated Profile
The LLM updates the "Products/Services" section to include the new AI-powered analytics offering while maintaining the rest of the profile.

## Integration with General Knowledge Mode

When a query like "What is this site about?" is asked:

1. Vector search retrieves top N chunks (raw content)
2. Also retrieves site profile (aggregate context)
3. If confidence < 70%, hybrid mode activates
4. Passes BOTH raw chunks AND site profile to LLM
5. LLM uses site profile for high-level understanding
6. LLM uses raw chunks for specific details
7. Combines both to generate comprehensive answer

## Files Modified

1. `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/content/service/ContextAggregationService.kt`
   - Added `incrementallyUpdateSiteProfile()` method (lines 107-189)
   - Added `getSiteProfile()` helper method (lines 194-234)
   - Added import for `ContentIngestionEvent` (line 9)

2. `/core-ms/core-ai/src/main/kotlin/ai/sovereignrag/content/event/ContextEnrichmentListener.kt`
   - Modified to call incremental updates on every ingestion (lines 46-48)

## Testing Recommendations

1. **Verify incremental updates trigger on every ingestion:**
   - Sync multiple WordPress pages
   - Check logs for "Incrementally updating site profile" messages
   - Verify one update per ingested document

2. **Verify profile quality:**
   - Query "What is this site about?" after each sync
   - Confirm response reflects newly added content
   - Ensure profile grows naturally without redundancy

3. **Verify performance:**
   - Monitor LLM response times for incremental updates
   - Should be faster than full regeneration
   - Should complete without blocking content ingestion

4. **Verify fallback to full generation:**
   - Clear all embeddings
   - Sync first document
   - Verify full profile generation occurs
   - Verify subsequent syncs use incremental updates

## Future Enhancements

1. **Rate Limiting:** Add optional delay between updates if LLM load becomes high
2. **Smart Triggers:** Only update if content is significantly different from existing profile
3. **Category-Specific Updates:** Extend incremental approach to category summaries
4. **Version History:** Track profile changes over time for debugging/analytics
5. **Profile Metrics:** Monitor profile quality (length, coverage, freshness)

## Related Documentation

- [Contextual RAG Implementation](./CONTEXTUAL_RAG_IMPLEMENTATION_COMPLETE.md)
- [Hybrid Mode Implementation](./PHASE5_COMPLETE.md)
- [Migration Plan](./MIGRATION_PLAN.md)
