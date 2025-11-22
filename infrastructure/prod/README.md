# Sovereign RAG - Kubernetes Production Deployment

This directory contains Kubernetes manifests for deploying Sovereign RAG in a production environment.

## Architecture

The deployment consists of the following components:

- **Sovereign RAG Application**: Spring Boot application (2+ replicas with HPA)
- **PostgreSQL**: Multi-tenant database with persistent storage
- **Redis**: Session and cache storage
- **Ollama**: Local LLM server for AI operations

## Prerequisites

1. **Kubernetes Cluster** (v1.25+)
   - Managed service: GKE, EKS, AKS, or DigitalOcean Kubernetes
   - Or self-hosted: kubeadm, k3s, etc.

2. **kubectl** configured to access your cluster

3. **kustomize** (v4.0+) or kubectl with built-in kustomize support

4. **Storage Class** available in your cluster
   - Update `storageClassName` in `03-pvc.yaml` to match your provider:
     - AWS: `gp3` or `gp2`
     - GCP: `pd-ssd` or `pd-standard`
     - Azure: `azure-disk`
     - DigitalOcean: `do-block-storage`

5. **Ingress Controller** (optional but recommended)
   - NGINX Ingress Controller
   - Traefik
   - Or cloud provider's load balancer

6. **Cert-Manager** (optional, for automatic TLS certificates)
   ```bash
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
   ```

## Pre-Deployment Configuration

### 1. Build and Push Docker Image

```bash
# Build the Docker image
docker build -t your-registry/sovereign-rag:v1.0.0 .

# Push to your container registry
docker push your-registry/sovereign-rag:v1.0.0
```

### 2. Update Configuration

#### Update Secrets (`02-secrets.yaml`)

**IMPORTANT**: Replace all placeholder values with production secrets:

```bash
# Generate secure passwords
openssl rand -base64 32  # PostgreSQL password
openssl rand -base64 32  # JWT secret

# Apply secrets manually (recommended for production)
kubectl create secret generic sovereign-rag-secrets \
  --namespace=sovereign-rag \
  --from-literal=POSTGRES_USER=sovereignrag \
  --from-literal=POSTGRES_PASSWORD=your-secure-password \
  --from-literal=JWT_SECRET=your-jwt-secret \
  --from-literal=SENDGRID_API_KEY=your-sendgrid-key \
  --dry-run=client -o yaml | kubectl apply -f -
```

#### Update Image Registry (`kustomization.yaml`)

```yaml
images:
- name: sovereign-rag
  newName: your-registry/sovereign-rag  # Replace with your actual registry
  newTag: v1.0.0  # Use semantic versioning
```

#### Update Domain (`21-sovereign-rag-service.yaml`)

Replace `sovereignrag.yourdomain.com` with your actual domain in the Ingress resource.

### 3. Update Storage Class (`03-pvc.yaml`)

```yaml
storageClassName: gp3  # Change to your cloud provider's storage class
```

## Deployment

### Option 1: Using kubectl with kustomize (Recommended)

```bash
# Deploy all resources
kubectl apply -k infrastructure/prod/

# Verify deployment
kubectl get all -n sovereign-rag

# Watch pods coming up
kubectl get pods -n sovereign-rag -w
```

### Option 2: Using plain kubectl

```bash
# Apply manifests in order
kubectl apply -f infrastructure/prod/00-namespace.yaml
kubectl apply -f infrastructure/prod/01-configmap.yaml
kubectl apply -f infrastructure/prod/02-secrets.yaml
kubectl apply -f infrastructure/prod/03-pvc.yaml
kubectl apply -f infrastructure/prod/10-postgresql.yaml
kubectl apply -f infrastructure/prod/11-redis.yaml
kubectl apply -f infrastructure/prod/12-ollama.yaml

# Wait for dependencies to be ready
kubectl wait --for=condition=ready pod -l app=postgresql -n sovereign-rag --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n sovereign-rag --timeout=300s
kubectl wait --for=condition=ready pod -l app=ollama -n sovereign-rag --timeout=600s

# Deploy application
kubectl apply -f infrastructure/prod/20-sovereign-rag-deployment.yaml
kubectl apply -f infrastructure/prod/21-sovereign-rag-service.yaml
kubectl apply -f infrastructure/prod/22-hpa.yaml
```

## Post-Deployment

### 1. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n sovereign-rag

# Check services
kubectl get svc -n sovereign-rag

# Check ingress
kubectl get ingress -n sovereign-rag

# View application logs
kubectl logs -f deployment/sovereign-rag -n sovereign-rag
```

### 2. Initialize Database

The application uses Flyway for database migrations. Migrations will run automatically on startup.

To verify:
```bash
kubectl logs -f deployment/sovereign-rag -n sovereign-rag | grep -i flyway
```

### 3. Test the Application

```bash
# Get the external IP/domain
kubectl get ingress -n sovereign-rag

# Test health endpoint
curl https://your-domain.com/actuator/health

# Test API (if accessible)
curl https://your-domain.com/api/v1/health
```

### 4. Ollama Model Loading

The Ollama init container will pull models on first startup. This can take 10-30 minutes depending on your internet speed.

Check progress:
```bash
kubectl logs -f deployment/ollama -n sovereign-rag -c pull-models
```

Required models:
- `gemma2:9b-instruct-q4_0` (~5GB)
- `jeffh/intfloat-multilingual-e5-large:q8_0` (~1GB)
- `llama3.2:1b` (~1GB)

## Scaling

### Manual Scaling

```bash
# Scale application
kubectl scale deployment sovereign-rag --replicas=5 -n sovereign-rag

# View HPA status
kubectl get hpa -n sovereign-rag
```

### Auto-Scaling

The HorizontalPodAutoscaler is configured to:
- Min replicas: 2
- Max replicas: 10
- Target CPU: 70%
- Target Memory: 80%

## Monitoring

### View Logs

```bash
# Application logs
kubectl logs -f deployment/sovereign-rag -n sovereign-rag

# PostgreSQL logs
kubectl logs -f deployment/postgresql -n sovereign-rag

# Ollama logs
kubectl logs -f deployment/ollama -n sovereign-rag

# All logs with timestamps
kubectl logs -f deployment/sovereign-rag -n sovereign-rag --timestamps=true
```

### Resource Usage

```bash
# Pod resource usage
kubectl top pods -n sovereign-rag

# Node resource usage
kubectl top nodes
```

## Backup and Restore

### PostgreSQL Backup

```bash
# Create backup
kubectl exec -it deployment/postgresql -n sovereign-rag -- \
  pg_dump -U sovereignrag sovereignrag_master > backup-$(date +%Y%m%d).sql

# Restore from backup
kubectl exec -i deployment/postgresql -n sovereign-rag -- \
  psql -U sovereignrag sovereignrag_master < backup-20250104.sql
```

### Persistent Volume Snapshots

If your cloud provider supports volume snapshots:

```bash
# Create PVC snapshot (GKE example)
kubectl create -f - <<EOF
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: postgresql-snapshot-$(date +%Y%m%d)
  namespace: sovereign-rag
spec:
  volumeSnapshotClassName: your-snapshot-class
  source:
    persistentVolumeClaimName: postgresql-pvc
EOF
```

## Troubleshooting

### Pods Not Starting

```bash
# Describe pod for events
kubectl describe pod <pod-name> -n sovereign-rag

# Check pod logs
kubectl logs <pod-name> -n sovereign-rag

# Check previous container logs if pod restarted
kubectl logs <pod-name> -n sovereign-rag --previous
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
kubectl exec -it deployment/postgresql -n sovereign-rag -- \
  psql -U sovereignrag -d sovereignrag_master -c "SELECT 1;"

# Check PostgreSQL logs
kubectl logs deployment/postgresql -n sovereign-rag
```

### Ollama Model Issues

```bash
# Check if models are loaded
kubectl exec -it deployment/ollama -n sovereign-rag -- \
  ollama list

# Manually pull a model
kubectl exec -it deployment/ollama -n sovereign-rag -- \
  ollama pull gemma2:9b-instruct-q4_0
```

### Application Errors

```bash
# Check application logs for errors
kubectl logs -f deployment/sovereign-rag -n sovereign-rag | grep ERROR

# Check Spring Boot actuator health
kubectl port-forward deployment/sovereign-rag 8000:8000 -n sovereign-rag
curl http://localhost:8000/actuator/health
```

## Security Considerations

1. **Secrets Management**
   - Use external secret management (e.g., AWS Secrets Manager, HashiCorp Vault)
   - Encrypt secrets at rest
   - Rotate secrets regularly

2. **Network Policies**
   - Implement NetworkPolicies to restrict pod-to-pod communication
   - Only allow necessary ingress/egress traffic

3. **RBAC**
   - Create service accounts with minimal permissions
   - Use PodSecurityPolicies or Pod Security Standards

4. **TLS/SSL**
   - Always use TLS for external traffic
   - Configure cert-manager for automatic certificate renewal

5. **Image Security**
   - Scan images for vulnerabilities
   - Use minimal base images (Alpine)
   - Run containers as non-root user

## Updates and Rollouts

### Rolling Update

```bash
# Update image
kubectl set image deployment/sovereign-rag \
  sovereign-rag=your-registry/sovereign-rag:v1.1.0 \
  -n sovereign-rag

# Watch rollout status
kubectl rollout status deployment/sovereign-rag -n sovereign-rag

# Rollback if needed
kubectl rollout undo deployment/sovereign-rag -n sovereign-rag
```

### Blue-Green Deployment

Use kustomize overlays for different environments:
```bash
kubectl apply -k infrastructure/prod/
kubectl apply -k infrastructure/staging/
```

## Cleanup

```bash
# Delete all resources
kubectl delete -k infrastructure/prod/

# Or delete namespace (removes everything)
kubectl delete namespace sovereign-rag
```

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kustomize Documentation](https://kustomize.io/)
- [Cert-Manager Documentation](https://cert-manager.io/docs/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)

## Support

For issues or questions, please open an issue on GitHub or contact the Sovereign RAG team.
