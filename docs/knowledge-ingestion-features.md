# Knowledge Ingestion Features Roadmap

This document outlines all planned data ingestion methods for the RAG platform, organized by implementation priority.

---

## Phase 1: Core Direct Upload (MVP)

### 1.1 File Upload
| Feature | Status | Description |
|---------|--------|-------------|
| Single File Upload | Planned | Upload individual files (PDF, DOCX, TXT, MD, CSV, JSON, XML) |
| Bulk File Upload | Planned | Multiple files in one operation |
| Folder Upload | Planned | Entire directory with nested structure preservation |
| Drag & Drop | Planned | Browser-based drag and drop interface |
| Text/Paste Input | Planned | Direct text input or clipboard paste |
| Q&A Pairs | Planned | Structured question-answer format for FAQ-style content |

**Supported File Types:**
- Documents: PDF, DOCX, DOC, ODT, RTF, TXT, MD
- Spreadsheets: CSV, XLSX, XLS, ODS
- Data: JSON, XML, YAML
- Code: All common programming languages
- Images (with OCR): PNG, JPG, JPEG, TIFF, BMP

### 1.2 Web Content
| Feature | Status | Description |
|---------|--------|-------------|
| URL/Link Ingestion | Planned | Single webpage content extraction |
| Sitemap Crawling | Planned | Parse XML sitemap and crawl all listed URLs |
| Domain Crawling | Planned | Recursive crawling with depth limits |
| RSS/Atom Feeds | Planned | Subscribe to and periodically ingest feed content |

---

## Phase 2: Sync & Automation

### 2.1 Sync Agents
| Feature | Status | Description |
|---------|--------|-------------|
| Desktop Sync Agent | Planned | Native app (Mac/Windows/Linux) that watches local folders |
| Remote Sync Server | Planned | Self-hosted sync service for server deployments |
| Watched Folders | Planned | Auto-sync on file changes (create/update/delete) |
| Scheduled Sync | Planned | Cron-based periodic synchronization |
| Selective Sync | Planned | Include/exclude patterns for file filtering |

### 2.2 Cloud Storage Integrations
| Feature | Status | Description |
|---------|--------|-------------|
| Google Drive | Planned | Sync from Drive folders with OAuth |
| Dropbox | Planned | Dropbox folder synchronization |
| OneDrive | Planned | Microsoft OneDrive integration |
| SharePoint | Planned | SharePoint document libraries |
| AWS S3 | Planned | S3 bucket sync with IAM credentials |
| Azure Blob Storage | Planned | Azure storage container sync |
| Google Cloud Storage | Planned | GCS bucket integration |
| Box | Planned | Box.com folder synchronization |

---

## Phase 3: APIs & SDKs

### 3.1 REST API
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/knowledge-sources` | POST | Upload single file |
| `/api/v1/knowledge-sources/bulk` | POST | Bulk upload multiple files |
| `/api/v1/knowledge-sources/url` | POST | Ingest from URL |
| `/api/v1/knowledge-sources/text` | POST | Ingest raw text content |
| `/api/v1/knowledge-sources/{id}` | PUT | Update existing source |
| `/api/v1/knowledge-sources/{id}` | DELETE | Remove source |
| `/api/v1/knowledge-sources/sync` | POST | Trigger manual sync |

### 3.2 SDKs
| SDK | Status | Description |
|-----|--------|-------------|
| Python SDK | Planned | `pip install sovereign-rag` |
| JavaScript/TypeScript SDK | Planned | `npm install @sovereign-rag/sdk` |
| Java SDK | Planned | Maven/Gradle package |
| Go SDK | Planned | `go get github.com/sovereign-rag/sdk-go` |
| CLI Tool | Planned | Command-line interface for scripting and automation |

**Python SDK Example:**
```python
from sovereign_rag import Client

client = Client(api_key="your-api-key")

# Upload a file
client.knowledge.upload("./document.pdf", knowledge_base_id="kb-123")

# Upload from URL
client.knowledge.ingest_url("https://example.com/page", knowledge_base_id="kb-123")

# Bulk upload
client.knowledge.upload_folder("./docs/", knowledge_base_id="kb-123")
```

**JavaScript SDK Example:**
```typescript
import { SovereignRAG } from '@sovereign-rag/sdk';

const client = new SovereignRAG({ apiKey: 'your-api-key' });

// Upload a file
await client.knowledge.upload(file, { knowledgeBaseId: 'kb-123' });

// Ingest URL
await client.knowledge.ingestUrl('https://example.com/page', { knowledgeBaseId: 'kb-123' });
```

---

## Phase 4: Webhooks & Event-Driven

### 4.1 Inbound Webhooks
| Feature | Status | Description |
|---------|--------|-------------|
| Generic Webhook Endpoint | Planned | Receive JSON payloads from any source |
| GitHub Webhooks | Planned | Auto-ingest on push, release, wiki changes |
| GitLab Webhooks | Planned | Repository event triggers |
| Bitbucket Webhooks | Planned | Code repository changes |
| Slack Events | Planned | Capture messages from channels |
| Custom Payload Transformers | Planned | Map arbitrary JSON to knowledge format |

### 4.2 Outbound Webhooks
| Feature | Status | Description |
|---------|--------|-------------|
| Ingestion Complete | Planned | Notify when document processing finishes |
| Sync Status | Planned | Sync success/failure notifications |
| Error Alerts | Planned | Processing error notifications |

---

## Phase 5: SaaS & Business App Integrations

### 5.1 Productivity & Documentation
| Integration | Status | Description |
|-------------|--------|-------------|
| Notion | Planned | Sync pages, databases, and workspaces |
| Confluence | Planned | Atlassian wiki spaces and pages |
| Coda | Planned | Document and table sync |
| Airtable | Planned | Base and table synchronization |
| Google Docs | Planned | Document sync via Drive API |

### 5.2 Support & Knowledge Management
| Integration | Status | Description |
|-------------|--------|-------------|
| Zendesk | Planned | Help center articles and tickets |
| Intercom | Planned | Help center and conversation data |
| Freshdesk | Planned | Knowledge base articles |
| HelpScout | Planned | Documentation and conversations |

### 5.3 CRM & Sales
| Integration | Status | Description |
|-------------|--------|-------------|
| Salesforce | Planned | Knowledge articles and case data |
| HubSpot | Planned | Knowledge base and blog content |
| Pipedrive | Planned | Notes and activity data |

### 5.4 Communication Platforms
| Integration | Status | Description |
|-------------|--------|-------------|
| Slack | Planned | Channel history, threads, canvas |
| Microsoft Teams | Planned | Channel conversations and files |
| Discord | Planned | Server and channel messages |
| Email (IMAP/SMTP) | Planned | Inbox ingestion with filters |
| Gmail API | Planned | Google Workspace email sync |

---

## Phase 6: Database Connectors

### 6.1 Relational Databases
| Connector | Status | Description |
|-----------|--------|-------------|
| PostgreSQL | Planned | Query-based ingestion with scheduled sync |
| MySQL/MariaDB | Planned | Table and view synchronization |
| SQL Server | Planned | Microsoft SQL database connector |
| Oracle | Planned | Enterprise Oracle DB support |
| SQLite | Planned | Local database file ingestion |

### 6.2 NoSQL & Document Stores
| Connector | Status | Description |
|-----------|--------|-------------|
| MongoDB | Planned | Collection and document sync |
| Elasticsearch | Planned | Index synchronization |
| CouchDB | Planned | Document database connector |
| DynamoDB | Planned | AWS NoSQL table sync |

### 6.3 Data Warehouses
| Connector | Status | Description |
|-----------|--------|-------------|
| Snowflake | Planned | Data warehouse queries |
| BigQuery | Planned | Google analytics and warehouse data |
| Redshift | Planned | AWS data warehouse connector |
| Databricks | Planned | Lakehouse data access |

---

## Phase 7: File Transfer & Network Protocols

### 7.1 Transfer Protocols
| Protocol | Status | Description |
|----------|--------|-------------|
| SFTP | Planned | Secure file transfer with SSH keys |
| FTP/FTPS | Planned | Standard and secure FTP |
| SCP | Planned | Secure copy over SSH |
| WebDAV | Planned | Web-based distributed authoring |
| SMB/CIFS | Planned | Windows network file shares |
| NFS | Planned | Network file system mounts |

---

## Phase 8: Streaming & Real-time Ingestion

### 8.1 Message Queues
| Platform | Status | Description |
|----------|--------|-------------|
| Apache Kafka | Planned | Event stream consumption |
| RabbitMQ | Planned | Message queue consumer |
| AWS SQS | Planned | Amazon queue service integration |
| Azure Service Bus | Planned | Microsoft message queue |
| Google Pub/Sub | Planned | GCP message streaming |
| Redis Pub/Sub | Planned | Real-time message streams |

### 8.2 Real-time Streams
| Feature | Status | Description |
|---------|--------|-------------|
| Server-Sent Events | Planned | SSE endpoint consumption |
| WebSocket Feeds | Planned | Real-time WebSocket data streams |

---

## Phase 9: Specialized Content Processing

### 9.1 Audio & Video
| Feature | Status | Description |
|---------|--------|-------------|
| Audio Transcription | Planned | Speech-to-text via Whisper/cloud APIs |
| Video Transcription | Planned | Extract audio and transcribe |
| YouTube Integration | Planned | Video transcript extraction |
| Podcast RSS | Planned | Audio feed transcription |

### 9.2 Image & Document Processing
| Feature | Status | Description |
|---------|--------|-------------|
| Image OCR | Planned | Extract text from images (Tesseract/cloud) |
| PDF OCR | Planned | Scanned document text extraction |
| Handwriting Recognition | Planned | Handwritten document processing |
| Table Extraction | Planned | Extract structured tables from documents |
| Chart/Graph Analysis | Planned | Visual data extraction |

---

## Phase 10: No-Code & Workflow Automation

### 10.1 Integration Platforms
| Platform | Status | Description |
|----------|--------|-------------|
| Zapier | Planned | Pre-built Zaps for 5000+ apps |
| Make (Integromat) | Planned | Visual workflow automation |
| n8n | Planned | Self-hosted workflow automation |
| Power Automate | Planned | Microsoft ecosystem automation |
| Tray.io | Planned | Enterprise workflow platform |
| Workato | Planned | Enterprise integration platform |

---

## Phase 11: Developer & Code Sources

### 11.1 Version Control
| Platform | Status | Description |
|----------|--------|-------------|
| GitHub | Planned | Repository code, docs, wikis, issues |
| GitLab | Planned | Full repository sync including CI configs |
| Bitbucket | Planned | Atlassian git repository sync |
| Azure DevOps | Planned | Microsoft repository integration |

### 11.2 Documentation Platforms
| Platform | Status | Description |
|----------|--------|-------------|
| ReadTheDocs | Planned | Documentation site crawling |
| GitBook | Planned | GitBook space synchronization |
| Docusaurus | Planned | Documentation site ingestion |
| MkDocs | Planned | Markdown documentation sites |
| Swagger/OpenAPI | Planned | API documentation ingestion |

---

## Phase 12: AI-Native Protocols

### 12.1 Model Context Protocol (MCP)

MCP enables AI models to directly access data sources through standardized server connections.

| Feature | Status | Description |
|---------|--------|-------------|
| MCP Server Implementation | Planned | Expose knowledge base as MCP server |
| MCP Client Integration | Planned | Connect to external MCP servers as data sources |
| Filesystem MCP | Planned | Local/remote file access |
| Database MCP | Planned | Direct database queries |
| Custom MCP Servers | Planned | User-defined MCP data connectors |

**MCP Server Capabilities:**
```json
{
  "name": "sovereign-rag-mcp",
  "version": "1.0.0",
  "capabilities": {
    "resources": true,
    "tools": true,
    "prompts": true
  },
  "tools": [
    {
      "name": "search_knowledge",
      "description": "Search the knowledge base",
      "parameters": {
        "query": "string",
        "knowledge_base_id": "string",
        "limit": "number"
      }
    },
    {
      "name": "ingest_content",
      "description": "Add content to knowledge base",
      "parameters": {
        "content": "string",
        "metadata": "object"
      }
    }
  ]
}
```

**Benefits:**
- Real-time bidirectional data access
- AI agents can both read AND write to knowledge base
- Standardized authentication/authorization
- Context-aware retrieval without batch sync

### 12.2 Agent-to-Agent Protocol (A2A)

Google's protocol for AI agent interoperability and task delegation.

| Feature | Status | Description |
|---------|--------|-------------|
| Agent Card Publication | Planned | Advertise RAG capabilities to other agents |
| Task Reception | Planned | Accept knowledge retrieval tasks from agents |
| Task Delegation | Planned | Delegate specialized retrieval to other agents |
| Multi-Agent Knowledge Sharing | Planned | Share context between agents |
| Federated RAG | Planned | Query multiple RAG systems across organizations |

**Agent Card Example:**
```json
{
  "name": "Sovereign RAG Agent",
  "description": "Knowledge retrieval and ingestion agent",
  "url": "https://api.sovereign-rag.ai/a2a",
  "capabilities": {
    "streaming": true,
    "pushNotifications": true
  },
  "skills": [
    {
      "id": "knowledge-search",
      "name": "Knowledge Search",
      "description": "Search across ingested knowledge bases"
    },
    {
      "id": "document-ingestion",
      "name": "Document Ingestion",
      "description": "Ingest new documents into knowledge base"
    }
  ]
}
```

**A2A Use Cases:**
- Primary agent delegates document retrieval to specialized RAG agent
- Agents share retrieved context across conversation
- Federated queries across multiple organizational RAG systems
- Agent discovery via published Agent Cards
- Push notifications for new knowledge availability

### 12.3 Other AI Standards
| Standard | Status | Description |
|----------|--------|-------------|
| OpenAI Function Calling | Planned | Tool definitions for GPT integration |
| LangChain Tools | Planned | Standardized tool interface |
| LlamaIndex Connectors | Planned | Framework data loaders |
| Semantic Kernel Plugins | Planned | Microsoft AI orchestration |
| AutoGen Agents | Planned | Multi-agent framework support |

---

## Phase 13: Enterprise & Legacy Systems

### 13.1 Enterprise Platforms
| Platform | Status | Description |
|----------|--------|-------------|
| SAP | Planned | ERP system document extraction |
| ServiceNow | Planned | ITSM knowledge articles |
| Workday | Planned | HR documentation |
| NetSuite | Planned | ERP knowledge content |

### 13.2 Legacy Systems
| System | Status | Description |
|--------|--------|-------------|
| IBM Notes/Domino | Planned | Legacy collaboration platform |
| Lotus Notes | Planned | Document database migration |
| Legacy File Servers | Planned | Windows Server file shares |
| Mainframe Extracts | Planned | Batch file processing |

### 13.3 Directory Services
| Service | Status | Description |
|---------|--------|-------------|
| LDAP | Planned | Directory information extraction |
| Active Directory | Planned | AD object and policy documentation |
| Okta | Planned | Identity provider documentation |

---

## Technical Architecture

### Microservice Responsibilities

```
┌─────────────────────────────────────────────────────────────────────┐
│                   knowledge-base module (core-ms)                    │
│  - KnowledgeBase entity (KB config, quotas, API keys)               │
│  - KnowledgeSource entity (document catalog/tracking)               │
│  - EmbeddingModel config (available models)                         │
│  - Region/Language configuration                                     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        ingestion-ms (scales independently)           │
│  - FileProcessor (extract content, create chunks)                   │
│  - WebScrapeProcessor (crawl URLs, extract content)                 │
│  - EmbeddingWorker (generate vectors, write to pgvector)            │
│  - PostgresJobQueue (distributed job processing)                    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PostgreSQL + pgvector                             │
│  kb_{id}.langchain4j_embeddings (LangChain4j schema)                │
│  kb_{id}.knowledge_sources (document tracking)                      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      agent-core (read-only retrieval)               │
│  - RetrievalService (similarity search)                             │
│  - ChatController (serve users)                                     │
│  - No embedding generation load                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Job Processing Flow

```
1. FILE_UPLOAD job created (status: UPLOADING)
   └─> Client uploads to S3 via presigned URL

2. Upload confirmed (status: QUEUED)
   └─> FileProcessor picks up job
       ├─> Extract content (Apache Tika)
       ├─> Create text chunks
       ├─> Create KnowledgeSource record
       └─> Create EMBEDDING jobs for chunk batches

3. EMBEDDING jobs (status: QUEUED)
   └─> EmbeddingWorker picks up job
       ├─> Generate embeddings (Ollama)
       ├─> Store in pgvector (LangChain4j schema)
       └─> Update KnowledgeSource status

4. Processing complete
   └─> KnowledgeSource status: READY
```

### LangChain4j PgVector Embedding Schema

The system uses LangChain4j's `PgVectorEmbeddingStore` for vector storage. The table is automatically created per knowledge base schema.

**Table: `kb_{id}.langchain4j_embeddings`**

| Column | Type | Description |
|--------|------|-------------|
| `embedding_id` | UUID | Primary key |
| `embedding` | vector(N) | Vector embedding (dimension from model) |
| `text` | TEXT | Original chunk text content |
| `metadata` | JSON | Chunk metadata (source_id, file_name, chunk_index, etc.) |

**Index:**
```sql
CREATE INDEX IF NOT EXISTS {table}_ivfflat_index
ON {table} USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

**Metadata Structure:**
```json
{
  "source_id": "uuid",
  "source_type": "FILE|URL|TEXT|QA",
  "file_name": "document.pdf",
  "chunk_index": 0,
  "total_chunks": 10,
  "created_at": "2025-01-01T00:00:00Z"
}
```

### Shared Interfaces (core-commons)

**EmbeddingGateway** - Vector storage operations:
```kotlin
interface EmbeddingGateway {
    fun storeEmbeddings(knowledgeBaseId: String, chunks: List<TextChunk>): List<String>
    fun deleteBySourceId(knowledgeBaseId: String, sourceId: String)
    fun search(knowledgeBaseId: String, queryEmbedding: FloatArray, maxResults: Int, minScore: Double): List<EmbeddingMatch>
}
```

**KnowledgeSourceGateway** - Document catalog operations:
```kotlin
interface KnowledgeSourceGateway {
    fun create(knowledgeBaseId: String, source: KnowledgeSourceRequest): KnowledgeSourceInfo
    fun updateStatus(knowledgeBaseId: String, sourceId: String, status: SourceStatus)
    fun findByKnowledgeBase(knowledgeBaseId: String): List<KnowledgeSourceInfo>
    fun delete(knowledgeBaseId: String, sourceId: String)
}
```

---

## Implementation Notes

### Authentication Methods Supported
- API Keys
- OAuth 2.0 (Authorization Code, Client Credentials)
- JWT Tokens
- Basic Auth
- AWS IAM Roles
- Azure Service Principal
- GCP Service Accounts
- SSH Keys (for SFTP/SCP)

### Processing Pipeline
1. **Ingestion** - Content acquisition from source
2. **Extraction** - Text and metadata extraction
3. **Chunking** - Intelligent document splitting
4. **Embedding** - Vector generation
5. **Indexing** - Storage in vector database
6. **Metadata Enrichment** - Entity extraction, classification

### Rate Limiting & Quotas
- Per-source rate limits to respect external API limits
- Configurable concurrent processing limits
- Queue-based processing for large batch operations
- Priority queues for real-time vs batch ingestion

### Error Handling
- Automatic retry with exponential backoff
- Dead letter queue for failed ingestions
- Detailed error reporting and logging
- Webhook notifications for failures

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-07 | Initial feature roadmap |
