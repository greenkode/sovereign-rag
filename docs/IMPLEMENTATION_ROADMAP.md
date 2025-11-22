# Implementation Roadmap

## Overview
This document provides a phased development plan to build Sovereign RAG from its current state to feature parity with Progress Agentic RAG.

---

## Current State Assessment

### ✅ Already Implemented
Based on the existing codebase:

1. **Multi-tenancy** - Database-per-tenant architecture
2. **Basic Document Ingestion** - File upload capability
3. **Vector Storage** - PgVector integration with LangChain4j
4. **LLM Integration** - Ollama support
5. **Embedding Generation** - Local embedding model support
6. **Basic Search** - Hybrid search (vector + full-text)
7. **Tenant Management** - Tenant registration and isolation
8. **Authentication** - JWT and API key auth

### ❌ Missing Features (From Screenshots)
1. Multiple upload types (Links, Sitemap, Q&A, Text)
2. Synchronization system (Folder, RSS, Sitemap)
3. Search configuration UI
4. Widget system
5. Multiple AI model providers (OpenAI, Anthropic, Google)
6. RAG evaluation metrics (REMI)
7. Activity logging
8. Label system
9. User management UI
10. Feedback collection
11. Advanced search options (query rephrasing, reranking)

---

## PHASE 1: CORE ENHANCEMENTS (Weeks 1-4)

### Goal
Enhance existing RAG capabilities and add essential upload types.

### Features

#### 1.1 Enhanced Document Ingestion
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] URL/Link upload endpoint
  - Implement HTML fetching and parsing
  - Extract text content from web pages
  - Handle metadata extraction (title, description, images)

- [ ] Manual text entry endpoint
  - Simple text block creation
  - Markdown support

- [ ] Sitemap crawler
  - XML sitemap parsing
  - Configurable max pages
  - URL filtering

- [ ] Q&A pairs upload
  - Structured question-answer format
  - Optimized chunking for Q&A

**Acceptance Criteria**:
- All 6 upload types functional (File, Folder, Links, Text, Sitemap, Q&A)
- Resources appear in Resources List
- Each type correctly processed and embedded

#### 1.2 Multiple AI Model Providers
**Priority**: P0 (Critical)
**Effort**: 2 weeks

**Tasks**:
- [ ] OpenAI integration (direct, not Azure)
  - API client implementation
  - Model selection (GPT-4o, GPT-4o-mini, etc.)
  - Token counting and cost tracking

- [ ] Anthropic Claude integration
  - API client
  - Support for Claude 3.5, Claude 4.5 models

- [ ] Google Gemini integration
  - API client
  - Support for Gemini Pro, Flash models

- [ ] Model configuration interface
  - Store model selections per Knowledge Box
  - Model parameters (temperature, max_tokens, etc.)

- [ ] Model switching in Search Configuration
  - Override default model per search config

**Acceptance Criteria**:
- At least 3 providers working (OpenAI, Anthropic, Ollama)
- Model selection persists in database
- Different models can be used for different tasks
- Graceful fallback when model unavailable

#### 1.3 Advanced Search Options
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Query rephrasing
  - Use LLM to rephrase user query for better retrieval
  - Configurable enable/disable
  - Custom rephrase prompt option

- [ ] Semantic re-ranking
  - Post-retrieval reranking with cross-encoder
  - Configurable threshold

- [ ] Reciprocal Rank Fusion
  - Combine keyword and vector search scores
  - Implement RRF algorithm

- [ ] Search result filtering
  - Pre-selected filters
  - Dynamic filter UI

**Acceptance Criteria**:
- Query rephrasing improves retrieval quality
- Reranking changes result order measurably
- RRF provides better combined results than either alone

---

## PHASE 2: CONFIGURATION & WIDGETS (Weeks 5-8)

### Goal
Implement search configuration system and embeddable widgets.

### Features

#### 2.1 Search Configuration System
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tasks**:
- [ ] SearchConfiguration entity and repository
- [ ] CRUD API endpoints for configurations
- [ ] Configuration cloning/templates
- [ ] Default configuration per Knowledge Box
- [ ] Configuration versioning

**Configuration Options to Implement**:
- [ ] Search options (use results, filters, rephrasing, reranking, RRF)
- [ ] Generative answer options (enable, model, prompts, token limits)
- [ ] Result display options (citations, metadata, thumbnails, top_k)
- [ ] User-intent routing (dynamic config selection)

**Acceptance Criteria**:
- Multiple configurations can be saved per Knowledge Box
- Configuration can be applied to searches
- UI reflects all configuration options

#### 2.2 Widget System
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tasks**:
- [ ] Widget entity and CRUD endpoints
- [ ] Widget configuration (styles, themes, placeholders)
- [ ] JavaScript widget SDK
  - Embed code generation
  - CDN hosting setup
  - Widget initialization script

- [ ] Widget rendering modes
  - Embedded in page
  - Chat mode
  - Popup modal

- [ ] Widget theming
  - Light theme
  - Dark theme
  - Custom CSS injection

- [ ] Chat history
  - Session management
  - LocalStorage persistence
  - Server-side session storage

**Acceptance Criteria**:
- Widget can be created from saved search config
- Embed code works on external websites
- All 3 widget styles functional
- Chat history persists across page reloads

#### 2.3 Feedback Collection
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Feedback entity and endpoints
- [ ] Widget feedback UI
  - Thumbs up/down buttons
  - Comment field
  - Detailed feedback on results

- [ ] Feedback analytics dashboard
- [ ] Link feedback to REMI metrics

**Acceptance Criteria**:
- Users can provide feedback on answers
- Feedback stored and retrievable
- Negative feedback highlights knowledge gaps

---

## PHASE 3: SYNCHRONIZATION (Weeks 9-11)

### Goal
Enable automatic content synchronization from external sources.

### Features

#### 3.1 Sync Agent Architecture
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] Sync server design (desktop agent + remote server)
- [ ] Sync job scheduler (cron-based)
- [ ] SyncConfiguration entity and API
- [ ] Sync status tracking and logging

#### 3.2 Folder Synchronization
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] File system monitoring
- [ ] Incremental sync (detect changes)
- [ ] File pattern matching (include/exclude)
- [ ] Bi-directional sync handling

**Acceptance Criteria**:
- Desktop agent can watch local folders
- Changes automatically uploaded
- Deleted files removed from Knowledge Box

#### 3.3 RSS & Sitemap Sync
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] RSS feed parser
- [ ] Sitemap crawler (recurring)
- [ ] Change detection (avoid re-indexing)
- [ ] Scheduled execution

**Acceptance Criteria**:
- RSS feeds auto-sync on schedule
- Sitemaps crawled without duplicates
- New content appears automatically

---

## PHASE 4: METRICS & ANALYTICS (Weeks 12-14)

### Goal
Implement RAG evaluation metrics and activity logging.

### Features

#### 4.1 Query Tracking
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Query entity comprehensive logging
- [ ] Performance metrics (response time, token usage)
- [ ] Session grouping for chat history
- [ ] IP and user agent tracking

#### 4.2 RAG Evaluation Metrics (REMI)
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] RAGMetrics entity and computation
- [ ] Answer relevance scoring
  - Use LLM as judge
  - Compare answer to query

- [ ] Context relevance scoring
  - Evaluate retrieved chunks against query

- [ ] Groundedness scoring
  - Check if answer is supported by context

- [ ] Metrics aggregation and visualization
- [ ] Time-series tracking
- [ ] Missing knowledge detection
  - Queries without answers
  - Low context relevance queries
  - Negative feedback queries

**Acceptance Criteria**:
- Metrics computed for each query
- Dashboard shows health status (avg, min, max)
- Performance evolution chart
- Missing knowledge reports actionable

#### 4.3 Activity Log
**Priority**: P3 (Premium feature)
**Effort**: 1 week

**Tasks**:
- [ ] Comprehensive activity logging
- [ ] Export functionality (CSV, JSON)
- [ ] Filtering and pagination
- [ ] Link to REMI metrics

**Acceptance Criteria**:
- All queries logged with full context
- Exportable for offline analysis
- Searchable and filterable

---

## PHASE 5: ADVANCED FEATURES (Weeks 15-18)

### Goal
Implement advanced classification, labeling, and organization features.

### Features

#### 5.1 Label System
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] LabelSet and Label entities
- [ ] Label CRUD endpoints
- [ ] Apply labels to Resources and TextBlocks
- [ ] Exclusive vs. multi-label support
- [ ] Label-based filtering in search
- [ ] Label auto-assignment (ML-based)

**Acceptance Criteria**:
- Labels can be created and managed
- Resources tagged with labels
- Search filters by labels
- Label sets enforce exclusivity rules

#### 5.2 Settings & Configuration UI
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Knowledge Box settings page
  - Name, description, slug
  - CORS configuration
  - IP whitelisting
  - Hidden resources toggle
  - Publish/unpublish

- [ ] AI models configuration page
  - Model selection per task type
  - Model parameters

- [ ] General settings
  - Zone/region selection
  - Storage limits

**Acceptance Criteria**:
- All settings editable via UI
- Settings persisted correctly
- CORS and IP restrictions enforced

#### 5.3 User Management
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] User invitation system
- [ ] Role-based access control (Reader, Writer, Manager)
- [ ] User list UI
- [ ] Role assignment and updates
- [ ] User removal

**Acceptance Criteria**:
- Users can be invited via email
- Roles enforced on API endpoints
- Users manageable via UI

#### 5.4 API Key Management
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] API key generation with roles
- [ ] Key listing (with prefix only)
- [ ] Key revocation
- [ ] Usage tracking (last used timestamp)

**Acceptance Criteria**:
- API keys created with secure generation
- Keys work for authentication
- Keys can be revoked
- Usage visible in UI

---

## PHASE 6: UI/UX IMPLEMENTATION (Weeks 19-24)

### Goal
Build complete admin dashboard matching Progress Agentic RAG UI.

### Features

#### 6.1 Core Navigation & Layout
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tech Stack Recommendation**:
- **Frontend**: React + TypeScript + Tailwind CSS
- **State Management**: React Query (TanStack Query)
- **Routing**: React Router
- **Forms**: React Hook Form + Zod validation
- **UI Components**: Radix UI or shadcn/ui

**Tasks**:
- [ ] Left sidebar navigation
- [ ] Top navigation bar
- [ ] Responsive layout
- [ ] Theme system (light/dark from screenshots)
- [ ] User avatar and profile menu

#### 6.2 Page Implementations
**Priority**: P1 (High)
**Effort**: 4 weeks

**Pages to Build**:
1. [ ] Home/Dashboard
2. [ ] Upload Data (with 6 upload types)
3. [ ] Resources List (with pagination, filtering)
4. [ ] Synchronize (sync configuration management)
5. [ ] Search (test interface with config panel)
6. [ ] Widgets (list, create, configure)
7. [ ] Settings (Knowledge Box config)
8. [ ] AI Models (model selection across tabs)
9. [ ] Metrics (REMI dashboard with charts)
10. [ ] Activity Log (query history table)
11. [ ] API Keys (key management)
12. [ ] Label Sets (label taxonomy management)
13. [ ] Users (user management table)

**Common Components**:
- [ ] Table with sorting, filtering, pagination
- [ ] Form inputs (text, select, toggle, radio)
- [ ] Modal dialogs
- [ ] Toast notifications
- [ ] Loading states
- [ ] Empty states
- [ ] Collapsible sections
- [ ] Tabs
- [ ] Code snippet display

**Acceptance Criteria**:
- All pages functional and styled
- Matches design from screenshots
- Responsive on mobile/tablet
- Accessible (ARIA labels, keyboard navigation)

---

## PHASE 7: PREMIUM FEATURES (Weeks 25-28)

### Goal
Implement advanced premium features for monetization.

### Features (Tier Gating)

#### 7.1 Agents System
**Priority**: P3 (Nice to have)
**Effort**: 3 weeks

**Description**: Multi-agent system for complex tasks.

**Tasks**:
- [ ] Agent orchestration framework
- [ ] Tool calling interface
- [ ] Agent memory and state
- [ ] Agent templates

#### 7.2 NER (Named Entity Recognition)
**Priority**: P3 (Nice to have)
**Effort**: 1 week

**Tasks**:
- [ ] Entity extraction from documents
- [ ] Entity type classification
- [ ] Entity-based search enhancement

#### 7.3 On-Demand Summarization
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] Document summarization API
- [ ] Configurable summary length
- [ ] Multi-document summarization

#### 7.4 Task Automation
**Priority**: P3 (Nice to have)
**Effort**: 2 weeks

**Tasks**:
- [ ] Automated labeling
- [ ] Automated Q&A generation from documents
- [ ] Global question answering across KB

---

## PHASE 8: PRODUCTION READINESS (Weeks 29-32)

### Goal
Prepare system for production deployment and scale.

### Features

#### 8.1 Performance Optimization
**Priority**: P0 (Critical)
**Effort**: 2 weeks

**Tasks**:
- [ ] Query optimization (database indices)
- [ ] Connection pool tuning
- [ ] Caching layer (Redis for search results, embeddings)
- [ ] Async processing for ingestion
- [ ] Batch embedding generation
- [ ] CDN for widget JavaScript
- [ ] Response compression

**Targets**:
- Search response < 2 seconds (current: ~1.8s)
- Ingestion < 50ms per chunk (current: ~25ms)
- Support 100 concurrent searches

#### 8.2 Monitoring & Observability
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] Application metrics (Prometheus)
- [ ] Distributed tracing (Jaeger/Zipkin)
- [ ] Logging aggregation (ELK stack or Loki)
- [ ] Health check endpoints
- [ ] Alert configuration

**Metrics to Track**:
- Request latency (p50, p95, p99)
- Error rates
- Token consumption
- Database query performance
- Queue depths

#### 8.3 Security Hardening
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] Rate limiting (by IP, by API key)
- [ ] Input validation and sanitization
- [ ] SQL injection prevention (use parameterized queries)
- [ ] XSS prevention in widget
- [ ] CORS enforcement
- [ ] API key rotation policy
- [ ] Secret management (HashiCorp Vault or AWS Secrets Manager)
- [ ] Audit logging

#### 8.4 Documentation
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Widget integration guide
- [ ] Admin user guide
- [ ] Developer documentation
- [ ] Deployment guide
- [ ] Troubleshooting guide

---

## DEPLOYMENT TIMELINE

### Month 1-2: Core Enhancements
- **Week 1-4**: Phase 1 (Document ingestion, AI models, advanced search)
- **Deliverable**: Multi-provider RAG system with 6 upload types

### Month 2-3: Configuration & Widgets
- **Week 5-8**: Phase 2 (Search configs, widgets, feedback)
- **Deliverable**: Embeddable widgets with customization

### Month 3: Synchronization
- **Week 9-11**: Phase 3 (Sync system)
- **Deliverable**: Auto-sync from folders, RSS, sitemaps

### Month 4: Analytics
- **Week 12-14**: Phase 4 (REMI, activity log)
- **Deliverable**: RAG evaluation and monitoring

### Month 4-5: Advanced Features
- **Week 15-18**: Phase 5 (Labels, settings, user mgmt)
- **Deliverable**: Enterprise-ready admin features

### Month 5-6: UI Implementation
- **Week 19-24**: Phase 6 (Full UI)
- **Deliverable**: Complete admin dashboard

### Month 7: Premium Features
- **Week 25-28**: Phase 7 (Agents, NER, etc.)
- **Deliverable**: Premium tier features

### Month 8: Production Launch
- **Week 29-32**: Phase 8 (Optimization, monitoring, docs)
- **Deliverable**: Production-ready system

---

## RESOURCE REQUIREMENTS

### Team Composition (Recommended)

1. **Backend Engineer** (2x)
   - Spring Boot/Kotlin expertise
   - LLM integration experience
   - Database optimization

2. **Frontend Engineer** (1x)
   - React/TypeScript
   - UI/UX implementation

3. **Full-Stack Engineer** (1x)
   - API + UI work
   - Widget development

4. **DevOps Engineer** (0.5x)
   - Infrastructure setup
   - CI/CD pipelines
   - Monitoring

5. **Product Manager** (0.5x)
   - Feature prioritization
   - User acceptance testing

### Infrastructure

**Development**:
- PostgreSQL (with pgvector)
- Redis (caching)
- Ollama (local LLM testing)
- Docker Compose

**Production** (estimate for 1000 concurrent users):
- PostgreSQL: 4 vCPU, 16GB RAM
- Application servers: 3x (4 vCPU, 8GB RAM each)
- Redis: 2 vCPU, 4GB RAM
- Load balancer
- CDN for widget JavaScript
- Object storage (S3) for documents

**External Services**:
- OpenAI API credits
- Anthropic API credits
- Google Cloud AI credits
- Email service (SendGrid/AWS SES)

---

## RISK MITIGATION

### High-Risk Areas

1. **Vector Search Performance**
   - **Risk**: Slow searches with large datasets
   - **Mitigation**: IVFFlat indexing, result caching, query optimization

2. **LLM API Costs**
   - **Risk**: Unpredictable token costs
   - **Mitigation**: Token limits, model selection strategy, caching answers

3. **Widget Security**
   - **Risk**: XSS attacks, data leakage
   - **Mitigation**: Content Security Policy, input sanitization, CORS restrictions

4. **Multi-Tenancy Isolation**
   - **Risk**: Cross-tenant data access
   - **Mitigation**: Row-level security, thorough testing, security audits

5. **Scaling Database per Tenant**
   - **Risk**: Connection pool exhaustion
   - **Mitigation**: Connection pooling tuning, database sharding plan

---

## SUCCESS METRICS

### Phase 1-2 (Months 1-2)
- ✅ All 6 upload types working
- ✅ 3+ AI model providers integrated
- ✅ Widgets embeddable on external sites

### Phase 3-4 (Months 3-4)
- ✅ Sync system processing 1000+ documents/hour
- ✅ REMI metrics computed for 95%+ of queries
- ✅ Average answer relevance > 0.80

### Phase 5-6 (Months 5-6)
- ✅ Full UI implemented matching screenshots
- ✅ 10+ active Knowledge Boxes in testing
- ✅ User feedback system capturing 20%+ response rate

### Phase 7-8 (Months 7-8)
- ✅ Production deployment successful
- ✅ Search latency < 2s at p95
- ✅ 99.9% uptime
- ✅ Documentation complete

---

## CONCLUSION

This 8-month roadmap provides a structured path to replicate the Progress Agentic RAG product. The phased approach allows for:

1. **Incremental Value**: Each phase delivers working features
2. **Risk Management**: Critical features prioritized early
3. **Flexibility**: Can adjust based on user feedback
4. **Quality**: Time for testing and optimization

**Next Steps**:
1. Review and approve roadmap
2. Assemble development team
3. Set up development environment
4. Begin Phase 1 implementation
