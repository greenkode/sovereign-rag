# Multilingual Embedding Model Migration Guide

## Overview

This guide covers the migration from `mxbai-embed-large` (English-only) to `jeffh/intfloat-multilingual-e5-large:q8_0` (100+ languages) for improved cross-lingual semantic search.

## Model Comparison

| Feature | mxbai-embed-large | multilingual-e5-large |
|---------|-------------------|----------------------|
| Dimension | 1024 | 1024 ✓ (compatible) |
| Languages | English only | 100+ languages |
| Size | 669 MB | 603 MB (q8_0) |
| Use Case | English documents | Multilingual queries |

## Configuration Changes

### Updated: `application.yml`

```yaml
embedding-model: ${OLLAMA_EMBEDDING_MODEL:jeffh/intfloat-multilingual-e5-large:q8_0}
```

**No dimension changes required** - both models use 1024-dimensional embeddings.

## Migration Strategy

### Option 1: Full Re-embedding (Recommended for Production)

Re-embed all existing documents with the new multilingual model to ensure optimal search quality.

**Steps:**

1. **Backup existing embeddings**
```sql
-- For each tenant database
CREATE TABLE langchain4j_embeddings_backup AS
SELECT * FROM langchain4j_embeddings;
```

2. **Clear existing embeddings**
```sql
TRUNCATE TABLE langchain4j_embeddings;
```

3. **Re-ingest all documents**
```bash
# Use your existing document ingestion API/process
# Documents will be automatically embedded with the new model
```

**Pros:**
- Best search quality across all languages
- Consistent embedding space
- Clean migration

**Cons:**
- Requires downtime or dual-mode operation
- Re-ingestion time depends on document volume

### Option 2: Gradual Migration (Zero-Downtime)

Continue using existing embeddings while new/updated documents use the new model.

**Steps:**

1. **Deploy new model configuration** (already done)
2. **New documents automatically use multilingual embeddings**
3. **Existing documents remain with old embeddings**
4. **Schedule gradual re-embedding** during low-traffic periods

**Pros:**
- Zero downtime
- Immediate benefits for new content

**Cons:**
- Mixed embedding spaces (lower search quality)
- Longer migration period

### Option 3: Hybrid Search (Advanced)

Maintain two separate embedding stores temporarily.

**Not recommended** - adds complexity without significant benefits.

## Testing the New Model

### 1. Verify Model is Downloaded

```bash
ollama list | grep multilingual-e5-large
```

Expected output:
```
jeffh/intfloat-multilingual-e5-large:q8_0   7ab6411cbbaf    603 MB    ...
```

### 2. Test Multilingual Embeddings

```bash
# Test Dutch query (previously problematic)
curl -X POST http://localhost:8000/api/v1/chat/ask \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-api-key-12345" \
  -d '{
    "query": "Waar gaat deze site over?",
    "sessionId": "test-session",
    "language": "nl"
  }'
```

### 3. Monitor Search Quality

Check logs for improved confidence scores:
```
[dev] Found 5 candidates
[dev] Re-ranking completed in 450ms, passing top 5 to LLM
[dev] Result 1 [0.7834]: ...  # Should see higher scores for multilingual queries
```

## Database Schema

No schema changes required. The `langchain4j_embeddings` table structure remains the same:

```sql
CREATE TABLE langchain4j_embeddings (
    id UUID PRIMARY KEY,
    embedding VECTOR(1024),  -- Same dimension
    text TEXT,
    metadata JSONB
);
```

## Performance Considerations

### Embedding Generation Speed

- **multilingual-e5-large**: ~300-400ms per query
- **mxbai-embed-large**: ~250-350ms per query
- **Difference**: +50-100ms (acceptable overhead for multilingual support)

### Model Size

- `q8_0` quantization: 603MB (recommended - best quality/size balance)
- `f16`: 1.1GB (higher precision, minimal quality gain)
- `f32`: 2.2GB (full precision, not recommended)

## Rollback Procedure

If issues arise, rollback to the previous model:

```bash
# Update application.yml or environment variable
export OLLAMA_EMBEDDING_MODEL=mxbai-embed-large

# Restart application
./restart-app.sh
```

If you cleared embeddings:
```sql
-- Restore from backup
INSERT INTO langchain4j_embeddings
SELECT * FROM langchain4j_embeddings_backup;
```

## Expected Benefits

1. **Multilingual Search**: Dutch, French, German, Spanish queries now work across English content
2. **Cross-Lingual RAG**: Query in one language, retrieve documents in another
3. **Better Low-Confidence Handling**: Fewer false negatives for non-English queries
4. **Reduced "No Results" Scenarios**: Especially for multilingual customer base

## Next Steps

1. ✅ Model downloaded and configured
2. ⏳ Choose migration strategy (Option 1 or Option 2)
3. ⏳ Test with sample multilingual queries
4. ⏳ Monitor search quality metrics
5. ⏳ Schedule full re-embedding (if Option 1)

## Additional Resources

- [Multilingual-E5 Paper](https://arxiv.org/abs/2402.05672)
- [Hugging Face Model Card](https://huggingface.co/intfloat/multilingual-e5-large)
- [Ollama Model Page](https://ollama.com/jeffh/intfloat-multilingual-e5-large)

## Support

For issues or questions:
- Check application logs: `/tmp/app-test-run.log`
- Monitor embedding generation time in ContentService logs
- Verify Ollama model availability: `ollama list`
