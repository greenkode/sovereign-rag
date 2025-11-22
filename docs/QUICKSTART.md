# Compilot AI - Quick Start Guide

## Prerequisites

1. **Java 21** installed and available in PATH
2. **PostgreSQL 17+** with pgvector extension
3. **Redis** running on `localhost:6379`
4. **Ollama** running on `http://localhost:11434` with models:
   - `llama3.2:3b`
   - `mxbai-embed-large`

## PostgreSQL + pgvector Setup

```bash
# Option 1: Docker
docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=compilot \
  -e POSTGRES_PASSWORD=RespectTheHangover \
  -e POSTGRES_DB=compilot_master \
  pgvector/pgvector:pg17

# Option 2: Local PostgreSQL installation
# Install PostgreSQL 17+ from https://www.postgresql.org/download/
# Install pgvector extension:
cd /tmp
git clone https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install

# Enable extension in PostgreSQL
psql -U postgres -c "CREATE EXTENSION vector;"
```

## Redis Setup

```bash
# Option 1: Docker
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:latest

# Option 2: Local installation
# macOS: brew install redis && brew services start redis
# Ubuntu: sudo apt install redis-server && sudo systemctl start redis
```

## Ollama Setup

```bash
# Install Ollama
curl https://ollama.ai/install.sh | sh

# Pull required models
ollama pull llama3.2:3b
ollama pull mxbai-embed-large

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

## Configuration

The application is pre-configured in `app/src/main/resources/application.yml` with defaults:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compilot_master?currentSchema=master
    username: compilot
    password: RespectTheHangover

  data:
    redis:
      host: localhost
      port: 6379

compilot:
  ollama:
    base-url: http://localhost:11434
    model: llama3.2:3b
    embedding-model: mxbai-embed-large
```

Or use environment variables:

```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/compilot_master?currentSchema=master
export POSTGRES_USER=compilot
export POSTGRES_PASSWORD=your-password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3.2:3b
export OLLAMA_EMBEDDING_MODEL=mxbai-embed-large
```

## Build and Run

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Or run the JAR
java -jar target/compilot-ai-0.0.1-SNAPSHOT.jar
```

The API will start on http://localhost:8000

## Development Tenant

On first startup, the application automatically creates a **development tenant** with the following credentials:

- **Tenant ID**: `dev`
- **API Key**: `dev-api-key-12345`

**⚠️ IMPORTANT**: Look for the startup logs that display the tenant credentials:

```
✓ Development tenant created successfully!
  Tenant ID: dev
  Tenant Name: Development Tenant
  Database: compilot_tenant_dev
  API Key: [generated-key]

Configure your WordPress plugin with:
  X-Tenant-ID: dev
  X-API-Key: [generated-key]
```

You can customize the dev tenant in `application.yml`:

```yaml
compilot:
  dev-tenant:
    enabled: true  # Set to false to disable auto-creation
    id: dev
    name: Development Tenant
    api-key: dev-api-key-12345  # Change for security
```

**For Production**: Disable dev tenant auto-creation:
```bash
export DEV_TENANT_ENABLED=false
```

## Verify Installation

```bash
# Health check
curl http://localhost:8000/health

# Expected response:
# {
#   "status": "healthy",
#   "database": "ok",
#   "llm": "ok",
#   "embedding": "ok"
# }
```

## Test the API

### 1. Ingest Content

```bash
curl -X POST http://localhost:8000/api/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Article",
    "content": "This is a test article about knowledge graphs and AI.",
    "url": "http://example.com/test",
    "post_type": "post"
  }'
```

### 2. Search Knowledge Graph

```bash
curl -X POST http://localhost:8000/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "knowledge graphs",
    "num_results": 5,
    "min_confidence": 0.5
  }'
```

### 3. Ask a Question

```bash
curl -X POST http://localhost:8000/api/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is a knowledge graph?",
    "persona": "customer_service"
  }'
```

### 4. Start Chat Session

```bash
# Start session
curl -X POST http://localhost:8000/api/agent/chat/start \
  -H "Content-Type: application/json" \
  -d '{"persona": "customer_service"}'

# Send message (replace {sessionId} with response from above)
curl -X POST http://localhost:8000/api/agent/chat/{sessionId}/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tell me about knowledge graphs",
    "use_general_knowledge": true
  }'
```

## Admin API Examples

### Get Statistics

```bash
curl http://localhost:8000/api/admin/stats
```

### List Entities

```bash
curl http://localhost:8000/api/admin/entities?page=0&page_size=20
```

### Get Graph Visualization

```bash
curl http://localhost:8000/api/admin/graph/visualization?limit=50
```

## WordPress Plugin Integration

1. Copy the `wordpress-plugin/compilot-ai-plugin` folder to your WordPress `wp-content/plugins/` directory
2. Activate the plugin in WordPress admin
3. Go to Settings → Compilot AI Assistant → Settings
4. Configure the connection:
   - **API Server URL (Backend)**: `http://host.docker.internal:8000` (for Docker) or `http://localhost:8000`
   - **API Server URL (Frontend)**: `http://localhost:8000`
   - **Tenant ID**: `dev` (from startup logs)
   - **API Key**: `dev-api-key-12345` (or the generated key from startup logs)
5. Save settings

The plugin will now:
- Automatically ingest new posts/pages to your isolated tenant database
- Provide a chat widget on your site
- Enable AI-powered search and question answering

## Next Steps

1. **Create Admin Dashboard**: Build a React/Next.js admin interface using the `/api/admin/*` endpoints
2. **Implement Entity Extraction**: Enhance the LLM-based entity extraction in `KnowledgeGraphService.kt:extractAndLinkEntities`
3. **Add Cross-Encoder Reranking**: Implement DJL-based reranking for improved search results
4. **Add Authentication**: Secure admin endpoints with JWT or OAuth2
5. **Production Deployment**: Deploy to production with Docker/Kubernetes

## Troubleshooting

### PostgreSQL Connection Issues
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs postgres

# Test connection
psql -U compilot -d compilot_master -c "SELECT version();"

# Verify pgvector extension
psql -U compilot -d compilot_master -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

### Redis Connection Issues
```bash
# Verify Redis is running
docker ps | grep redis

# Test connection
redis-cli ping  # Should return PONG
```

### Ollama Issues
```bash
# Check if Ollama is running
ps aux | grep ollama

# Restart Ollama
ollama serve

# List available models
ollama list
```

### Build Issues
```bash
# Clean build
./mvnw clean

# Rebuild
./mvnw clean package -DskipTests

# Check Java version
java -version  # Should be 21+
```

## Support

For issues or questions, please check:
- [MIGRATION_PLAN.md](MIGRATION_PLAN.md) - Migration details
- [README.md](README.md) - Full documentation
- [GitHub Issues](https://github.com/your-org/compilot-ai/issues)
