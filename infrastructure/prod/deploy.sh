#!/bin/bash

# Compilot AI Production Deployment Script
# This script helps deploy Compilot AI to a Kubernetes cluster

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="compilot-ai"
TIMEOUT="600s"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl."
        exit 1
    fi

    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Check your kubeconfig."
        exit 1
    fi

    log_info "Prerequisites check passed."
}

check_secrets() {
    log_warn "Checking if secrets are configured..."

    # Check if placeholder values are still present
    if grep -q "CHANGE_ME_IN_PRODUCTION" 02-secrets.yaml; then
        log_error "Secrets still contain placeholder values!"
        log_error "Please update 02-secrets.yaml with production secrets before deploying."
        log_error "Generate secure secrets with: openssl rand -base64 32"
        exit 1
    fi

    log_info "Secrets appear to be configured."
}

create_namespace() {
    log_info "Creating namespace..."
    kubectl apply -f 00-namespace.yaml
}

deploy_infrastructure() {
    log_info "Deploying infrastructure components..."

    # Deploy config and secrets
    kubectl apply -f 01-configmap.yaml
    kubectl apply -f 02-secrets.yaml

    # Deploy persistent volumes
    kubectl apply -f 03-pvc.yaml

    log_info "Waiting for PVCs to be bound..."
    kubectl wait --for=jsonpath='{.status.phase}'=Bound pvc/postgresql-pvc -n $NAMESPACE --timeout=$TIMEOUT || true
    kubectl wait --for=jsonpath='{.status.phase}'=Bound pvc/redis-pvc -n $NAMESPACE --timeout=$TIMEOUT || true
    kubectl wait --for=jsonpath='{.status.phase}'=Bound pvc/ollama-models-pvc -n $NAMESPACE --timeout=$TIMEOUT || true
}

deploy_databases() {
    log_info "Deploying PostgreSQL..."
    kubectl apply -f 10-postgresql.yaml

    log_info "Waiting for PostgreSQL to be ready..."
    kubectl wait --for=condition=ready pod -l app=postgresql -n $NAMESPACE --timeout=$TIMEOUT

    log_info "Deploying Redis..."
    kubectl apply -f 11-redis.yaml

    log_info "Waiting for Redis to be ready..."
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=$TIMEOUT
}

deploy_ollama() {
    log_info "Deploying Ollama..."
    kubectl apply -f 12-ollama.yaml

    log_warn "Ollama init container will pull models. This may take 10-30 minutes..."
    log_info "You can check progress with: kubectl logs -f deployment/ollama -n $NAMESPACE -c pull-models"

    # Don't wait for Ollama to be ready as it can take a long time
    log_warn "Continuing deployment. Ollama will become ready in the background."
}

deploy_application() {
    log_info "Deploying Compilot AI application..."
    kubectl apply -f 20-compilot-ai-deployment.yaml
    kubectl apply -f 21-compilot-ai-service.yaml
    kubectl apply -f 22-hpa.yaml

    log_info "Waiting for application to be ready..."
    kubectl wait --for=condition=ready pod -l app=compilot-ai -n $NAMESPACE --timeout=$TIMEOUT || {
        log_warn "Application pods not ready yet. Check logs with: kubectl logs -f deployment/compilot-ai -n $NAMESPACE"
    }
}

show_status() {
    log_info "Deployment Status:"
    echo ""
    kubectl get all -n $NAMESPACE
    echo ""

    log_info "Ingress:"
    kubectl get ingress -n $NAMESPACE
    echo ""

    log_info "PVCs:"
    kubectl get pvc -n $NAMESPACE
    echo ""
}

show_logs() {
    log_info "Recent application logs:"
    kubectl logs deployment/compilot-ai -n $NAMESPACE --tail=50 || log_warn "Application not ready yet"
}

show_access_info() {
    log_info "Access Information:"

    # Get ingress URL
    INGRESS_HOST=$(kubectl get ingress compilot-ai -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "Not configured")

    if [ "$INGRESS_HOST" != "Not configured" ]; then
        log_info "External URL: https://$INGRESS_HOST"
        log_info "Health Check: https://$INGRESS_HOST/actuator/health"
    else
        log_warn "Ingress not configured. Using port-forward for access:"
        log_info "Run: kubectl port-forward svc/compilot-ai 8000:80 -n $NAMESPACE"
        log_info "Then access: http://localhost:8000/actuator/health"
    fi
}

# Main deployment flow
main() {
    log_info "Starting Compilot AI Production Deployment..."
    echo ""

    check_prerequisites
    check_secrets

    create_namespace
    deploy_infrastructure
    deploy_databases
    deploy_ollama
    deploy_application

    log_info "Deployment complete!"
    echo ""

    show_status
    show_access_info

    echo ""
    log_info "Useful commands:"
    echo "  Watch pods:        kubectl get pods -n $NAMESPACE -w"
    echo "  View logs:         kubectl logs -f deployment/compilot-ai -n $NAMESPACE"
    echo "  Check HPA:         kubectl get hpa -n $NAMESPACE"
    echo "  Port forward:      kubectl port-forward svc/compilot-ai 8000:80 -n $NAMESPACE"
    echo ""

    log_info "Deployment script finished successfully!"
}

# Handle script arguments
case "${1:-deploy}" in
    deploy)
        main
        ;;
    status)
        show_status
        show_access_info
        ;;
    logs)
        show_logs
        ;;
    delete)
        log_warn "Deleting all resources in namespace $NAMESPACE..."
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            kubectl delete namespace $NAMESPACE
            log_info "Namespace deleted."
        else
            log_info "Deletion cancelled."
        fi
        ;;
    *)
        echo "Usage: $0 {deploy|status|logs|delete}"
        echo ""
        echo "Commands:"
        echo "  deploy  - Deploy all resources (default)"
        echo "  status  - Show deployment status"
        echo "  logs    - Show application logs"
        echo "  delete  - Delete all resources"
        exit 1
        ;;
esac
