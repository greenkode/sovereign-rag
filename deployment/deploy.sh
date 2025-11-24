#!/bin/bash

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║       SovereignRAG Self-Hosted Deployment Script              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env exists
if [ ! -f .env ]; then
    print_error ".env file not found!"
    print_info "Creating .env from .env.example..."
    cp .env.example .env
    print_warning "Please edit .env file with your configuration before continuing."
    print_warning "Required changes:"
    echo "  - POSTGRES_PASSWORD"
    echo "  - REDIS_PASSWORD"
    echo "  - JWT_SECRET"
    echo "  - SOVEREIGNRAG_LICENSE_KEY"
    exit 1
fi

# Load environment variables
source .env

# Validate required variables
print_info "Validating configuration..."

if [ "$POSTGRES_PASSWORD" = "CHANGE_ME_STRONG_PASSWORD" ]; then
    print_error "Please set POSTGRES_PASSWORD in .env file"
    exit 1
fi

if [ "$REDIS_PASSWORD" = "CHANGE_ME_REDIS_PASSWORD" ]; then
    print_error "Please set REDIS_PASSWORD in .env file"
    exit 1
fi

if [ "$JWT_SECRET" = "CHANGE_ME_JWT_SECRET_MIN_32_CHARS_REQUIRED" ]; then
    print_error "Please set JWT_SECRET in .env file"
    print_info "Generate with: openssl rand -base64 32"
    exit 1
fi

if [ "$SOVEREIGNRAG_LICENSE_KEY" = "your-license-key-here" ]; then
    print_warning "License key not set. Application will run in trial mode."
fi

# Check Docker
print_info "Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

print_info "Docker installation verified ✓"

# Check if services are already running
print_info "Checking for existing services..."
if docker-compose ps | grep -q "Up"; then
    print_warning "Services are already running."
    read -p "Do you want to restart? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Stopping existing services..."
        docker-compose down
    else
        print_info "Keeping existing services running."
        exit 0
    fi
fi

# Pull images
print_info "Pulling Docker images..."
docker-compose pull

# Make scripts executable
chmod +x scripts/*.sh

# Start services
print_info "Starting services..."
docker-compose up -d

# Wait for services to be healthy
print_info "Waiting for services to be healthy..."
echo -n "Checking"

max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    echo -n "."

    if docker-compose ps | grep -q "unhealthy"; then
        echo ""
        print_error "Some services are unhealthy. Check logs with: docker-compose logs"
        exit 1
    fi

    # Check if all services are healthy
    healthy_count=$(docker-compose ps | grep -c "healthy" || true)
    if [ "$healthy_count" -ge 3 ]; then
        echo ""
        print_info "All services are healthy! ✓"
        break
    fi

    sleep 2
    attempt=$((attempt + 1))
done

if [ $attempt -eq $max_attempts ]; then
    echo ""
    print_warning "Services took longer than expected to start. Check status with: docker-compose ps"
fi

# Download Ollama models
print_info "Downloading Ollama models (this may take a while)..."
docker exec sovereignrag-ollama ollama pull ${OLLAMA_MODEL}
docker exec sovereignrag-ollama ollama pull ${OLLAMA_EMBEDDING_MODEL}

# Run database migrations
print_info "Running database migrations..."
docker-compose exec -T identity-ms java -jar /app.jar || print_warning "Identity-MS migrations may have already run"
docker-compose exec -T audit-ms java -jar /app.jar || print_warning "Audit-MS migrations may have already run"
docker-compose exec -T core-ms java -jar /app.jar || print_warning "Core-MS migrations may have already run"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                   Deployment Successful! 🎉                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
print_info "Services are running:"
echo "  • Core API:     http://localhost:${CORE_PORT}"
echo "  • Identity API: http://localhost:${IDENTITY_PORT}"
echo "  • Audit API:    http://localhost:${AUDIT_PORT}"
echo "  • Ollama API:   http://localhost:${OLLAMA_PORT}"
echo ""
print_info "Management commands:"
echo "  • View logs:     docker-compose logs -f"
echo "  • Stop services: docker-compose down"
echo "  • Check status:  docker-compose ps"
echo ""
print_info "API Documentation:"
echo "  • Swagger UI: http://localhost:${IDENTITY_PORT}/swagger-ui.html"
echo ""
