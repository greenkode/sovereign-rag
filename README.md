# Sovereign RAG - Multi-Tenant RAG System

A multi-tenant Retrieval Augmented Generation (RAG) system built with Spring Boot, LangChain4j, PostgreSQL with pgvector, and Kotlin.

## Overview

Sovereign RAG provides:
- **Vector Search**: PostgreSQL with pgvector extension for semantic search and document storage
- **Multi-Tenancy**: Isolated databases per tenant with dynamic routing
- **Semantic Search**: Ollama embeddings for vector similarity search
- **Conversational AI**: LangChain4j-powered chat agents with personas
- **Contextual RAG**: Incremental site profile updates with hybrid knowledge mode
- **REST API**: WordPress plugin compatible endpoints
- **Admin Dashboard**: Unanswered query and escalation management

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Kotlin 1.9.25
- **LLM Framework**: LangChain4j 0.35.0
- **Database**: PostgreSQL 17+ with pgvector extension
- **Cache**: Redis
- **LLM Provider**: Ollama (local)
- **Build Tool**: Maven

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 17+ with pgvector extension installed
- Redis (for caching and session storage)
- Ollama (running on http://localhost:11434)
  - Models: `llama3.2:3b`, `mxbai-embed-large`

## Configuration

Create `application.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sovereignrag_master?currentSchema=master
    username: sovereignrag
    password: your-password

  data:
    redis:
      host: localhost
      port: 6379

sovereignrag:
  ollama:
    base-url: http://localhost:11434
    model: llama3.2:3b
    embedding-model: mxbai-embed-large
```

## Running the Application

### Development

```bash
./mvnw spring-boot:run
```

### Production

```bash
./mvnw clean package
java -jar target/sovereign-rag-0.0.1-SNAPSHOT.jar
```

The API will be available at `http://localhost:8000`

## API Endpoints

### Main API (WordPress Plugin Compatible)

- `POST /api/ingest` - Ingest content into knowledge graph
- `POST /api/search` - Semantic search
- `POST /api/ask` - Ask a question (RAG)
- `POST /api/agent/chat/start` - Start chat session
- `POST /api/agent/chat/{sessionId}/message` - Send chat message
- `GET /api/autocomplete` - Entity autocomplete
- `GET /api/stats` - Knowledge graph statistics
- `GET /health` - Health check

### Admin API (for React/Next.js Dashboard)

- `GET /api/admin/stats` - System statistics
- `GET /api/admin/entities` - List entities (paginated)
- `GET /api/admin/entities/{uuid}` - Get entity details
- `GET /api/admin/episodes` - List episodes (paginated)
- `GET /api/admin/relationships` - List relationships (paginated)
- `GET /api/admin/graph/visualization` - Graph visualization data
- `GET /api/admin/config` - System configuration
- `DELETE /api/admin/entities/{uuid}` - Delete entity
- `DELETE /api/admin/episodes/{uuid}` - Delete episode

## WordPress Plugin Integration

The WordPress plugin is located at `wordpress-plugin/graphiti-knowledge-graph/`.

### Installation

1. Copy the plugin folder to your WordPress `wp-content/plugins/` directory
2. Activate the plugin in WordPress admin
3. Configure the API URL: `http://localhost:8000` (or your production URL)

The plugin provides:
- Chat widget for website visitors
- Content ingestion on post publish/update
- Knowledge graph search shortcode

## Admin Dashboard (React/Next.js)

The admin dashboard should be created as a separate Next.js project that consumes the `/api/admin/*` endpoints.

Recommended features:
- Entity, Episode, and Relationship management
- Graph visualization (using D3.js or vis.js)
- Ingestion monitoring
- Configuration management
- Analytics and statistics

Example admin dashboard structure:

```
admin-dashboard/
├── app/
│   ├── dashboard/
│   ├── entities/
│   ├── episodes/
│   ├── relationships/
│   ├── graph-viz/
│   └── settings/
├── components/
└── lib/
```

## Development

### Project Structure

```
src/main/kotlin/ai/sovereignrag/
├── config/          # Spring Boot configuration
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # JPA repositories
├── domain/          # Domain models
├── dto/             # Request/Response DTOs
└── util/            # Utilities
```

### Building

```bash
./mvnw clean install
```

### Testing

```bash
./mvnw test
```

## Architecture

This system uses a multi-tenant architecture with PostgreSQL and pgvector for vector search. See [docs/MIGRATION_PLAN.md](docs/MIGRATION_PLAN.md) for the migration history from Neo4j to PostgreSQL.

### Key Features

- **Multi-Tenancy**: Each tenant gets an isolated PostgreSQL database
- **Vector Search**: pgvector extension for efficient similarity search with 1024-dimensional embeddings
- **Hybrid Search**: Combines vector similarity (70%) with full-text search (30%)
- **Dynamic Routing**: Tenant database connections are created and cached on-demand
- **Schema Management**: Flyway migrations for both master and tenant databases

### API Compatibility

All endpoints maintain 100% compatibility with the WordPress plugin. No changes required to the plugin.

## License

[Your License]

## Contributing

[Your Contributing Guidelines]
