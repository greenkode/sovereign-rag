# SovereignRAG Self-Hosted Deployment Guide

Welcome to SovereignRAG! This guide will help you deploy the complete RAG platform on your infrastructure.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start (Docker Compose)](#quick-start-docker-compose)
- [Production Deployment (Kubernetes)](#production-deployment-kubernetes)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Backup & Recovery](#backup--recovery)
- [Troubleshooting](#troubleshooting)
- [Support](#support)

## Prerequisites

### Hardware Requirements

**Minimum Requirements:**
- **CPU:** 8 cores
- **RAM:** 16 GB
- **Storage:** 200 GB SSD
- **GPU:** Optional (recommended for better LLM performance)

**Recommended for Production:**
- **CPU:** 16+ cores
- **RAM:** 32+ GB
- **Storage:** 500+ GB SSD
- **GPU:** NVIDIA GPU with 16+ GB VRAM

### Software Requirements

- **Docker:** 24.0+ and Docker Compose 2.0+
- **Kubernetes:** 1.27+ (for enterprise deployment)
- **Helm:** 3.12+ (for Kubernetes deployment)
- **PostgreSQL:** 16+ (if using external database)

### License Key

Contact [sales@sovereignrag.ai](mailto:sales@sovereignrag.ai) to obtain your license key.

## Quick Start (Docker Compose)

Perfect for development, testing, and small deployments.

### 1. Download Deployment Package

```bash
git clone https://github.com/sovereignrag/deployment.git
cd deployment
```

### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env file with your configuration
nano .env
```

**Required Changes:**
```bash
POSTGRES_PASSWORD=your_strong_password_here
REDIS_PASSWORD=your_redis_password_here
JWT_SECRET=$(openssl rand -base64 32)
SOVEREIGNRAG_LICENSE_KEY=your_license_key_here
```

### 3. Deploy

```bash
# Make deployment script executable
chmod +x deploy.sh

# Run deployment
./deploy.sh
```

The script will:
- ✅ Validate configuration
- ✅ Pull Docker images
- ✅ Start all services
- ✅ Download LLM models
- ✅ Run database migrations
- ✅ Verify health

### 4. Access Services

Once deployed, services are available at:

- **Core API:** http://localhost:8000
- **Identity API:** http://localhost:9093
- **Swagger UI:** http://localhost:9093/swagger-ui.html
- **Audit API:** http://localhost:9080

### 5. Create First Tenant

```bash
# Using the API
curl -X POST http://localhost:9093/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "id": "acme",
    "name": "Acme Corporation",
    "subscriptionTier": "ENTERPRISE"
  }'
```

## Production Deployment (Kubernetes)

For enterprise deployments with high availability and auto-scaling.

### 1. Prerequisites

```bash
# Verify Kubernetes cluster
kubectl cluster-info

# Install Helm
helm version
```

### 2. Add Helm Repository

```bash
helm repo add sovereignrag https://charts.sovereignrag.ai
helm repo update
```

### 3. Prepare Values File

```bash
# Create custom values
cat > sovereignrag-values.yaml <<EOF
global:
  license:
    key: "your-license-key-here"

postgresql:
  auth:
    password: "strong-postgres-password"

redis:
  auth:
    password: "strong-redis-password"

corems:
  ingress:
    enabled: true
    hosts:
      - host: api.yourdomain.com
        paths:
          - path: /
            pathType: Prefix
    tls:
      - secretName: sovereignrag-tls
        hosts:
          - api.yourdomain.com

identityms:
  ingress:
    enabled: true
    hosts:
      - host: identity.yourdomain.com
        paths:
          - path: /
            pathType: Prefix

# Enable monitoring
monitoring:
  enabled: true
  grafana:
    adminPassword: "grafana-password"
EOF
```

### 4. Install Chart

```bash
# Create namespace
kubectl create namespace sovereignrag

# Install SovereignRAG
helm install sovereignrag sovereignrag/sovereignrag \
  -f sovereignrag-values.yaml \
  --namespace sovereignrag
```

### 5. Verify Deployment

```bash
# Check pods
kubectl get pods -n sovereignrag

# Check services
kubectl get svc -n sovereignrag

# Check ingress
kubectl get ingress -n sovereignrag
```

### 6. Access Services

```bash
# Get external IP
kubectl get ingress -n sovereignrag

# Access via configured domain
# https://api.yourdomain.com
# https://identity.yourdomain.com
```

## Configuration

### Environment Variables

#### Core Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SOVEREIGNRAG_LICENSE_KEY` | Your license key | - | Yes |
| `POSTGRES_PASSWORD` | PostgreSQL password | - | Yes |
| `REDIS_PASSWORD` | Redis password | - | Yes |
| `JWT_SECRET` | JWT signing secret | - | Yes |

#### Database Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `sovereignrag` |
| `POSTGRES_USER` | Database user | `sovereignrag` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |

#### LLM Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `OLLAMA_MODEL` | Main LLM model | `gpt-oss:20b-cloud` |
| `OLLAMA_EMBEDDING_MODEL` | Embedding model | `jeffh/intfloat-multilingual-e5-large:q8_0` |
| `OLLAMA_GUARDRAIL_MODEL` | Guardrail model | `gpt-oss:20b-cloud` |

#### Application Features

| Variable | Description | Default |
|----------|-------------|---------|
| `ADMIN_ENABLED` | Enable admin features | `true` |
| `GUARDRAILS_ENABLED` | Enable guardrails | `true` |
| `USE_RERANKING` | Enable result re-ranking | `true` |
| `DEV_TENANT_ENABLED` | Auto-create dev tenant | `false` |

### Read Replica Configuration

The deployment automatically configures read replicas for horizontal scaling.

**Docker Compose:**
- Primary database: `postgres`
- Read replica: `postgres-replica`

**Kubernetes:**
```yaml
postgresql:
  readReplicas:
    replicaCount: 2  # Number of read replicas
```

All read-only queries (`@Transactional(readOnly=true)`) automatically route to replicas.

## Monitoring

### Docker Compose (Production)

```bash
# Use production compose file with monitoring
docker-compose -f docker-compose.prod.yml up -d

# Access monitoring
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000
```

### Kubernetes

Monitoring is enabled by default with Prometheus and Grafana.

```bash
# Access Grafana
kubectl port-forward svc/sovereignrag-grafana 3000:3000 -n sovereignrag

# Login: admin / <GRAFANA_PASSWORD>
# Default dashboards:
# - SovereignRAG Overview
# - Database Performance
# - API Metrics
```

### Health Checks

```bash
# Core MS
curl http://localhost:8000/actuator/health

# Identity MS
curl http://localhost:9093/actuator/health

# Audit MS
curl http://localhost:9080/actuator/health
```

## Backup & Recovery

### Database Backup (Docker Compose)

```bash
# Automated backup script
cat > backup.sh <<'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec sovereignrag-postgres pg_dump \
  -U sovereignrag sovereignrag \
  > backups/backup_${DATE}.sql
echo "Backup created: backups/backup_${DATE}.sql"
EOF

chmod +x backup.sh
./backup.sh
```

### Database Restore

```bash
# Restore from backup
docker exec -i sovereignrag-postgres psql \
  -U sovereignrag sovereignrag \
  < backups/backup_20250124_120000.sql
```

### Kubernetes Backup

```bash
# Create backup job
kubectl create job pg-backup \
  --from=cronjob/postgresql-backup \
  -n sovereignrag

# Download backup
kubectl cp sovereignrag/pg-backup-xxx:/backups/backup.sql ./backup.sql
```

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs -f

# Or specific service
docker-compose logs -f core-ms

# Check health
docker-compose ps
```

### Database Connection Issues

```bash
# Test database connection
docker exec sovereignrag-postgres psql \
  -U sovereignrag -d sovereignrag -c "SELECT 1"

# Check schemas
docker exec sovereignrag-postgres psql \
  -U sovereignrag -d sovereignrag \
  -c "\dn"
```

### License Issues

```bash
# Check license status in logs
docker-compose logs core-ms | grep -i license

# Verify license key is set
docker-compose exec core-ms env | grep LICENSE
```

### Performance Issues

```bash
# Check resource usage
docker stats

# Check database performance
docker exec sovereignrag-postgres psql \
  -U sovereignrag -d sovereignrag \
  -c "SELECT * FROM pg_stat_activity"
```

### Common Error Messages

| Error | Solution |
|-------|----------|
| `License has expired` | Contact sales for license renewal |
| `Connection refused` | Check if PostgreSQL is running |
| `Out of memory` | Increase Docker memory limit |
| `Model not found` | Wait for Ollama to download models |

## Upgrading

### Docker Compose

```bash
# Pull new images
docker-compose pull

# Restart with new version
docker-compose up -d

# Check migration status
docker-compose logs core-ms | grep -i migration
```

### Kubernetes

```bash
# Update Helm chart
helm upgrade sovereignrag sovereignrag/sovereignrag \
  -f sovereignrag-values.yaml \
  --namespace sovereignrag
```

## Scaling

### Docker Compose

```bash
# Scale Core MS replicas
docker-compose up -d --scale core-ms=3

# Scale Identity MS replicas
docker-compose up -d --scale identity-ms=2
```

### Kubernetes

```bash
# Manual scaling
kubectl scale deployment sovereignrag-core-ms \
  --replicas=5 -n sovereignrag

# Or update values.yaml
helm upgrade sovereignrag sovereignrag/sovereignrag \
  --set corems.replicaCount=5 \
  --namespace sovereignrag
```

Auto-scaling is enabled by default in Kubernetes based on CPU/memory usage.

## Security Checklist

- [ ] Change default passwords
- [ ] Generate strong JWT secret
- [ ] Configure firewall rules
- [ ] Enable HTTPS/TLS
- [ ] Regular backups scheduled
- [ ] Monitor logs for suspicious activity
- [ ] Keep dependencies updated
- [ ] Restrict database access
- [ ] Use secrets management (Vault, AWS Secrets Manager)

## Support

### Community Support

- **Documentation:** https://docs.sovereignrag.ai
- **GitHub Discussions:** https://github.com/sovereignrag/discussions
- **Discord:** https://discord.gg/sovereignrag

### Enterprise Support

Contact: [support@sovereignrag.ai](mailto:support@sovereignrag.ai)

**Include in support requests:**
- License key
- Deployment method (Docker/Kubernetes)
- Version information
- Relevant logs
- Error messages

---

**SovereignRAG** - Your Data, Your Infrastructure, Your Control
