# API Specifications

## Overview
This document defines the complete REST API for the Sovereign RAG system based on the Progress Agentic RAG product analysis.

**Base URL**: `https://api.sovereignrag.ai/v1`

**Authentication**: All requests require either:
- Bearer token (JWT) for web UI
- API Key in `X-API-Key` header for programmatic access

---

## 1. KNOWLEDGE BOX MANAGEMENT

### 1.1 List Knowledge Boxes
```http
GET /knowledge-boxes
Authorization: Bearer {token}
```

**Response 200**:
```json
{
  "knowledge_boxes": [
    {
      "id": "1eeffe49-7e69-4807-9a30-df883a0b9de4",
      "slug": "first-box",
      "name": "First Box",
      "description": "My first knowledge box",
      "zone": "europe-1",
      "status": "private",
      "created_at": "2025-01-15T10:30:00Z",
      "last_modified_at": "2025-01-20T14:22:00Z"
    }
  ],
  "total": 1
}
```

### 1.2 Create Knowledge Box
```http
POST /knowledge-boxes
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "slug": "my-kb",
  "name": "My Knowledge Box",
  "description": "Description here",
  "zone": "europe-1"
}
```

**Response 201**:
```json
{
  "id": "uuid",
  "slug": "my-kb",
  "name": "My Knowledge Box",
  "api_key": "srag_1234567890abcdef...",
  "message": "Knowledge Box created successfully. Save the API key - it won't be shown again!"
}
```

### 1.3 Get Knowledge Box
```http
GET /knowledge-boxes/{kb_id}
Authorization: Bearer {token}
```

### 1.4 Update Knowledge Box Settings
```http
PATCH /knowledge-boxes/{kb_id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Updated Name",
  "description": "Updated description",
  "enable_hidden_resources": false,
  "allowed_origins": ["https://example.com", "https://app.example.com"],
  "allowed_ip_addresses": ["192.168.1.0/24"]
}
```

### 1.5 Publish/Unpublish Knowledge Box
```http
POST /knowledge-boxes/{kb_id}/publish
POST /knowledge-boxes/{kb_id}/unpublish
Authorization: Bearer {token}
```

---

## 2. RESOURCE MANAGEMENT

### 2.1 Upload File
```http
POST /knowledge-boxes/{kb_id}/resources/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data**:
- `file`: Binary file data
- `title` (optional): Custom title
- `metadata` (optional): JSON string

**Response 202**:
```json
{
  "resource_id": "uuid",
  "status": "processing",
  "message": "File uploaded successfully and queued for processing"
}
```

### 2.2 Upload from URL
```http
POST /knowledge-boxes/{kb_id}/resources/from-url
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "url": "https://example.com/document.pdf",
  "title": "Optional Title",
  "metadata": {
    "category": "documentation"
  }
}
```

### 2.3 Upload Text Resource
```http
POST /knowledge-boxes/{kb_id}/resources/text
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "title": "Manual Entry",
  "content": "The text content here...",
  "metadata": {}
}
```

### 2.4 Upload Q&A Pairs
```http
POST /knowledge-boxes/{kb_id}/resources/qa-pairs
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "pairs": [
    {
      "question": "What is RAG?",
      "answer": "Retrieval Augmented Generation is..."
    },
    {
      "question": "How does it work?",
      "answer": "It combines search with LLMs..."
    }
  ]
}
```

### 2.5 Upload Sitemap
```http
POST /knowledge-boxes/{kb_id}/resources/sitemap
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "sitemap_url": "https://example.com/sitemap.xml",
  "max_pages": 100,
  "follow_links": true
}
```

### 2.6 List Resources
```http
GET /knowledge-boxes/{kb_id}/resources
Authorization: Bearer {token}

Query Parameters:
  - page: int (default 1)
  - page_size: int (default 20, max 100)
  - type: ResourceType (optional filter)
  - status: ResourceStatus (optional filter)
  - search: string (optional text search)
```

**Response 200**:
```json
{
  "resources": [
    {
      "id": "uuid",
      "title": "My Document",
      "type": "document",
      "source_type": "upload",
      "status": "indexed",
      "file_size": 1024000,
      "mime_type": "application/pdf",
      "thumbnail_url": "https://...",
      "metadata": {},
      "indexed_at": "2025-01-15T12:00:00Z",
      "created_at": "2025-01-15T11:45:00Z"
    }
  ],
  "total": 42,
  "page": 1,
  "page_size": 20
}
```

### 2.7 Get Resource Details
```http
GET /knowledge-boxes/{kb_id}/resources/{resource_id}
Authorization: Bearer {token}
```

### 2.8 Delete Resource
```http
DELETE /knowledge-boxes/{kb_id}/resources/{resource_id}
Authorization: Bearer {token}
```

---

## 3. SYNCHRONIZATION

### 3.1 Create Sync Configuration
```http
POST /knowledge-boxes/{kb_id}/sync-configurations
Authorization: Bearer {token}
Content-Type: application/json
```

**Request (Folder Sync)**:
```json
{
  "name": "My Documents Folder",
  "sync_type": "folder",
  "sync_agent_type": "desktop_agent",
  "source_config": {
    "path": "/Users/username/Documents",
    "include_patterns": ["*.pdf", "*.docx"],
    "exclude_patterns": ["**/archive/**"]
  },
  "schedule_cron": "0 */6 * * *"
}
```

**Request (RSS Sync)**:
```json
{
  "name": "Company Blog",
  "sync_type": "rss",
  "sync_agent_type": "remote_server",
  "source_config": {
    "feed_url": "https://example.com/feed.xml",
    "max_items": 50
  },
  "schedule_cron": "0 * * * *"
}
```

**Request (Sitemap Sync)**:
```json
{
  "name": "Website Content",
  "sync_type": "sitemap",
  "sync_agent_type": "remote_server",
  "source_config": {
    "sitemap_url": "https://example.com/sitemap.xml",
    "max_pages": 200,
    "follow_links": false
  },
  "schedule_cron": "0 0 * * *"
}
```

### 3.2 List Sync Configurations
```http
GET /knowledge-boxes/{kb_id}/sync-configurations
Authorization: Bearer {token}
```

### 3.3 Trigger Manual Sync
```http
POST /knowledge-boxes/{kb_id}/sync-configurations/{sync_id}/trigger
Authorization: Bearer {token}
```

### 3.4 Get Sync Logs
```http
GET /knowledge-boxes/{kb_id}/sync-configurations/{sync_id}/logs
Authorization: Bearer {token}

Query Parameters:
  - limit: int (default 20)
  - offset: int (default 0)
```

---

## 4. SEARCH & QUERY

### 4.1 Search (Test/Debug)
```http
POST /knowledge-boxes/{kb_id}/search
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "query": "What is retrieval augmented generation?",
  "configuration_id": "uuid", // Optional, use saved config
  "options": {
    "use_search_results": true,
    "rephrase_query": true,
    "semantic_reranking": true,
    "generate_answer": true,
    "display_mode": "show_citations",
    "top_k": 5
  }
}
```

**Response 200**:
```json
{
  "query": "What is retrieval augmented generation?",
  "rephrased_query": "Explain the RAG technique in natural language processing",
  "answer": {
    "text": "Retrieval Augmented Generation (RAG) is...",
    "model": "gpt-4o",
    "reasoning_steps": ["Step 1...", "Step 2..."],
    "tokens_used": 450
  },
  "search_results": [
    {
      "resource_id": "uuid",
      "resource_title": "RAG Introduction",
      "text_block_id": "uuid",
      "content": "RAG is a technique that combines...",
      "similarity_score": 0.92,
      "is_citation": true,
      "metadata": {}
    }
  ],
  "citations": [
    {
      "index": 1,
      "resource_id": "uuid",
      "text_block_id": "uuid",
      "snippet": "..."
    }
  ],
  "performance": {
    "total_time_ms": 1820,
    "search_time_ms": 150,
    "rerank_time_ms": 300,
    "llm_time_ms": 1350
  }
}
```

### 4.2 Widget Search (Public API)
```http
POST /public/widgets/{widget_id}/search
X-API-Key: {api_key}
Content-Type: application/json
```

**Request**:
```json
{
  "query": "How do I reset my password?",
  "session_id": "uuid", // For chat history
  "filters": {} // Optional preselected filters
}
```

**Response**: Same structure as 4.1

---

## 5. SEARCH CONFIGURATIONS

### 5.1 Create Search Configuration
```http
POST /knowledge-boxes/{kb_id}/search-configurations
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Standard RAG Answer",
  "description": "Default configuration for general queries",
  "use_search_results": true,
  "rephrase_query": true,
  "semantic_reranking": true,
  "reciprocal_rank_fusion": false,
  "generate_answer": true,
  "specific_model_id": null,
  "display_mode": "show_citations",
  "citation_threshold": 0.7,
  "display_results": true,
  "display_thumbnails": true,
  "limit_top_k": 10,
  "is_default": true
}
```

**Response 201**:
```json
{
  "id": "uuid",
  "name": "Standard RAG Answer",
  "created_at": "2025-01-15T10:00:00Z"
}
```

### 5.2 List Search Configurations
```http
GET /knowledge-boxes/{kb_id}/search-configurations
Authorization: Bearer {token}
```

### 5.3 Get Search Configuration
```http
GET /knowledge-boxes/{kb_id}/search-configurations/{config_id}
Authorization: Bearer {token}
```

### 5.4 Update Search Configuration
```http
PATCH /knowledge-boxes/{kb_id}/search-configurations/{config_id}
Authorization: Bearer {token}
```

---

## 6. WIDGETS

### 6.1 Create Widget
```http
POST /knowledge-boxes/{kb_id}/widgets
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Support Chat Widget",
  "search_configuration_id": "uuid",
  "widget_style": "chat_mode",
  "widget_theme": "light",
  "search_bar_placeholder": "Ask a question...",
  "chat_bar_placeholder": "Type your message...",
  "enable_chat_history": true,
  "persist_chat_history": true,
  "feedback_mode": "global_feedback",
  "collapse_text_blocks": false
}
```

**Response 201**:
```json
{
  "id": "uuid",
  "name": "Support Chat Widget",
  "embed_code": "<script src=\"https://cdn.sovereignrag.ai/widget.js\" data-widget-id=\"uuid\"></script>",
  "created_at": "2025-01-15T10:00:00Z"
}
```

### 6.2 List Widgets
```http
GET /knowledge-boxes/{kb_id}/widgets
Authorization: Bearer {token}
```

### 6.3 Get Widget
```http
GET /knowledge-boxes/{kb_id}/widgets/{widget_id}
Authorization: Bearer {token}
```

### 6.4 Update Widget
```http
PATCH /knowledge-boxes/{kb_id}/widgets/{widget_id}
Authorization: Bearer {token}
```

### 6.5 Get Widget Embed Code
```http
GET /knowledge-boxes/{kb_id}/widgets/{widget_id}/embed
Authorization: Bearer {token}
```

**Response 200**:
```json
{
  "widget_id": "uuid",
  "embed_code": "<script>...</script>",
  "cdn_url": "https://cdn.sovereignrag.ai/widget.js"
}
```

---

## 7. AI MODELS

### 7.1 List Available Models
```http
GET /ai-models
Authorization: Bearer {token}

Query Parameters:
  - task_type: AITaskType (optional filter)
  - provider: AIProvider (optional filter)
```

**Response 200**:
```json
{
  "models": [
    {
      "id": "gpt-4o",
      "name": "OpenAI + Azure ChatGPT-4o",
      "provider": "openai_azure",
      "task_types": ["answer_generation"],
      "parameters": {
        "max_tokens": 4096,
        "temperature": 0.7
      },
      "is_premium": false
    },
    {
      "id": "claude-sonnet-4.5",
      "name": "Anthropic Claude 4.5 Sonnet",
      "provider": "anthropic",
      "task_types": ["answer_generation"],
      "is_premium": true
    }
  ]
}
```

### 7.2 Configure AI Model for Knowledge Box
```http
POST /knowledge-boxes/{kb_id}/ai-model-configurations
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "task_type": "answer_generation",
  "provider": "openai_azure",
  "model_name": "gpt-4o",
  "model_parameters": {
    "temperature": 0.7,
    "max_tokens": 2000
  }
}
```

### 7.3 List AI Model Configurations
```http
GET /knowledge-boxes/{kb_id}/ai-model-configurations
Authorization: Bearer {token}
```

---

## 8. METRICS & ANALYTICS

### 8.1 Get RAG Metrics (REMI)
```http
GET /knowledge-boxes/{kb_id}/metrics/remi
Authorization: Bearer {token}

Query Parameters:
  - period: string (e.g., "last_7_days", "last_30_days", "2025-01")
  - metric: string (optional: "answer_relevance", "context_relevance", "groundedness")
```

**Response 200**:
```json
{
  "period": "last_7_days",
  "health_status": {
    "answer_relevance": {
      "average": 0.82,
      "min": 0.45,
      "max": 0.98
    },
    "context_relevance": {
      "average": 0.75,
      "min": 0.30,
      "max": 0.95
    },
    "groundedness": {
      "average": 0.88,
      "min": 0.60,
      "max": 1.0
    }
  },
  "performance_evolution": [
    {
      "date": "2025-01-15",
      "answer_relevance": 0.85,
      "context_relevance": 0.78,
      "groundedness": 0.90
    }
  ]
}
```

### 8.2 Get Missing Knowledge
```http
GET /knowledge-boxes/{kb_id}/metrics/missing-knowledge
Authorization: Bearer {token}

Query Parameters:
  - period: string
  - type: string ("no_answer" | "low_context_relevance" | "negative_feedback")
  - page: int
  - page_size: int
```

**Response 200**:
```json
{
  "queries": [
    {
      "query_id": "uuid",
      "query": "How do I configure SSO?",
      "created_at": "2025-01-15T10:00:00Z",
      "context_relevance": 0.15,
      "has_answer": false
    }
  ],
  "total": 12,
  "page": 1
}
```

### 8.3 Activity Log (Premium)
```http
GET /knowledge-boxes/{kb_id}/activity-log
Authorization: Bearer {token}

Query Parameters:
  - start_date: ISO8601
  - end_date: ISO8601
  - page: int
  - page_size: int
```

**Response 200**:
```json
{
  "queries": [
    {
      "id": "uuid",
      "query": "What is RAG?",
      "answer_generated": true,
      "tokens_consumed": 450,
      "response_time_ms": 1820,
      "feedback_rating": "positive",
      "created_at": "2025-01-15T10:00:00Z"
    }
  ],
  "total": 1523,
  "page": 1
}
```

### 8.4 Export Activity Log
```http
GET /knowledge-boxes/{kb_id}/activity-log/export
Authorization: Bearer {token}

Query Parameters:
  - start_date: ISO8601
  - end_date: ISO8601
  - format: string ("csv" | "json")
```

**Response 200**: CSV or JSON file download

---

## 9. FEEDBACK

### 9.1 Submit Feedback
```http
POST /knowledge-boxes/{kb_id}/queries/{query_id}/feedback
X-API-Key: {api_key}
Content-Type: application/json
```

**Request**:
```json
{
  "feedback_type": "answer_only",
  "rating": "positive",
  "comment": "Very helpful, thanks!"
}
```

**Response 201**:
```json
{
  "id": "uuid",
  "message": "Feedback recorded successfully"
}
```

---

## 10. LABELS

### 10.1 Create Label Set
```http
POST /knowledge-boxes/{kb_id}/label-sets
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Document Types",
  "apply_to": "resources",
  "is_exclusive": true,
  "labels": [
    {"name": "Technical Documentation", "color": "#3B82F6"},
    {"name": "Marketing Materials", "color": "#10B981"},
    {"name": "Legal Documents", "color": "#F59E0B"}
  ]
}
```

### 10.2 List Label Sets
```http
GET /knowledge-boxes/{kb_id}/label-sets
Authorization: Bearer {token}
```

### 10.3 Apply Label to Resource
```http
POST /knowledge-boxes/{kb_id}/resources/{resource_id}/labels
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "label_id": "uuid"
}
```

---

## 11. USER MANAGEMENT

### 11.1 List Users
```http
GET /knowledge-boxes/{kb_id}/users
Authorization: Bearer {token}
```

**Response 200**:
```json
{
  "users": [
    {
      "id": "uuid",
      "name": "Umoh Bassey-Duke",
      "email": "umoh@sku.africa",
      "role": "manager",
      "added_at": "2025-01-10T09:00:00Z"
    }
  ]
}
```

### 11.2 Add User
```http
POST /knowledge-boxes/{kb_id}/users
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "email": "newuser@example.com",
  "role": "reader"
}
```

**Response 201**:
```json
{
  "message": "User invited successfully. An invitation email has been sent."
}
```

### 11.3 Update User Role
```http
PATCH /knowledge-boxes/{kb_id}/users/{user_id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "role": "writer"
}
```

### 11.4 Remove User
```http
DELETE /knowledge-boxes/{kb_id}/users/{user_id}
Authorization: Bearer {token}
```

---

## 12. API KEYS

### 12.1 Create API Key
```http
POST /knowledge-boxes/{kb_id}/api-keys
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Production API Key",
  "role": "reader"
}
```

**Response 201**:
```json
{
  "id": "uuid",
  "name": "Production API Key",
  "key": "srag_1234567890abcdefghijklmnopqrstuvwxyz",
  "role": "reader",
  "message": "Save this key - it won't be shown again!"
}
```

### 12.2 List API Keys
```http
GET /knowledge-boxes/{kb_id}/api-keys
Authorization: Bearer {token}
```

**Response 200**:
```json
{
  "api_keys": [
    {
      "id": "uuid",
      "name": "Production API Key",
      "key_prefix": "srag_123",
      "role": "reader",
      "is_active": true,
      "last_used_at": "2025-01-15T10:00:00Z",
      "created_at": "2025-01-10T09:00:00Z"
    }
  ]
}
```

### 12.3 Revoke API Key
```http
DELETE /knowledge-boxes/{kb_id}/api-keys/{key_id}
Authorization: Bearer {token}
```

---

## 13. WEBHOOKS (Future Feature)

### 13.1 Create Webhook
```http
POST /knowledge-boxes/{kb_id}/webhooks
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**:
```json
{
  "url": "https://example.com/webhook",
  "events": ["resource.indexed", "query.completed"],
  "secret": "your-webhook-secret"
}
```

---

## 14. ERROR RESPONSES

All errors follow this structure:

```json
{
  "error": {
    "code": "error_code",
    "message": "Human-readable error message",
    "details": {} // Optional additional context
  }
}
```

### Common Error Codes:

| Status | Code | Description |
|--------|------|-------------|
| 400 | `invalid_request` | Malformed request body or parameters |
| 401 | `unauthorized` | Missing or invalid authentication |
| 403 | `forbidden` | Insufficient permissions |
| 404 | `not_found` | Resource doesn't exist |
| 409 | `conflict` | Duplicate resource or constraint violation |
| 422 | `validation_error` | Request validation failed |
| 429 | `rate_limit_exceeded` | Too many requests |
| 500 | `internal_error` | Server error |
| 503 | `service_unavailable` | Temporary service disruption |

---

## 15. RATE LIMITS

| Tier | Requests/minute | Requests/day |
|------|-----------------|--------------|
| Free/Trial | 60 | 1,000 |
| Basic | 300 | 10,000 |
| Pro | 1,000 | 100,000 |
| Enterprise | Custom | Custom |

Rate limit headers included in responses:
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1642345678
```

---

## Next Document

**IMPLEMENTATION_ROADMAP.md** - Phased development plan with milestones
