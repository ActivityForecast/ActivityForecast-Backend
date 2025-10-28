#!/bin/bash

# ActivityForecast Backend Deployment Script
# This script handles the deployment of the application on Oracle Cloud instance

set -e  # Exit on any error

# Configuration
PROJECT_NAME="activityforecast"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env"
BACKUP_DIR="/opt/backups"
LOG_FILE="/opt/logs/deploy.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Create necessary directories
create_directories() {
    log "Creating necessary directories..."
    mkdir -p "$BACKUP_DIR"
    mkdir -p "$(dirname "$LOG_FILE")"
    mkdir -p logs/app
    mkdir -p logs/nginx
    mkdir -p ssl
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed"
    fi
    
    if ! docker info &> /dev/null; then
        error "Docker daemon is not running"
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        error "Docker Compose is not installed"
    fi
    
    # Check if .env file exists
    if [ ! -f "$ENV_FILE" ]; then
        error "Environment file $ENV_FILE not found"
    fi
    
    success "Prerequisites check passed"
}

# Create SSL certificates (self-signed for development)
create_ssl_certificates() {
    log "Checking SSL certificates..."
    
    if [ ! -f "ssl/cert.pem" ] || [ ! -f "ssl/key.pem" ]; then
        warning "SSL certificates not found. Creating self-signed certificates..."
        
        # Create self-signed certificate
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout ssl/key.pem \
            -out ssl/cert.pem \
            -subj "/C=KR/ST=Seoul/L=Seoul/O=ActivityForecast/CN=localhost"
        
        success "Self-signed SSL certificates created"
    else
        log "SSL certificates already exist"
    fi
}

# Backup current deployment
backup_current_deployment() {
    log "Creating backup of current deployment..."
    
    BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_PATH="$BACKUP_DIR/backup_$BACKUP_TIMESTAMP"
    
    mkdir -p "$BACKUP_PATH"
    
    # Database backup removed for development efficiency
    # Data is preserved through Docker volumes (mysql_prod_data)
    
    # Backup application logs
    if [ -d "logs" ]; then
        cp -r logs "$BACKUP_PATH/"
        log "Application logs backed up"
    fi
    
    success "Backup completed: $BACKUP_PATH"
}

# Stop current services
stop_services() {
    log "Stopping current services..."
    
    if [ -f "$COMPOSE_FILE" ]; then
        docker-compose -f "$COMPOSE_FILE" down --remove-orphans || true
        success "Services stopped"
    else
        warning "Compose file not found, skipping service stop"
    fi
}

# Pull latest images
pull_images() {
    log "Pulling latest Docker images..."
    
    # Pull images defined in docker-compose
    docker-compose -f "$COMPOSE_FILE" pull
    
    success "Docker images pulled"
}

# Start services
start_services() {
    log "Starting services..."
    
    # Start services with production compose file
    docker-compose -f "$COMPOSE_FILE" up -d
    
    success "Services started"
}

# Health check
health_check() {
    log "Performing health check..."
    
    # Wait for services to start
    sleep 30
    
    # Check if containers are running
    if ! docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        error "Some containers are not running"
    fi
    
    # Check application health endpoint
    MAX_RETRIES=10
    RETRY_COUNT=0
    
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -f http://localhost:8080/test/health &> /dev/null; then
            success "Application health check passed"
            return 0
        fi
        
        RETRY_COUNT=$((RETRY_COUNT + 1))
        log "Health check attempt $RETRY_COUNT/$MAX_RETRIES failed, retrying..."
        sleep 10
    done
    
    error "Application health check failed after $MAX_RETRIES attempts"
}

# Cleanup old resources
cleanup() {
    log "Cleaning up old resources..."
    
    # Remove unused Docker images
    docker image prune -f &> /dev/null || true
    
    # Remove old backups (keep last 7 days)
    find "$BACKUP_DIR" -type d -name "backup_*" -mtime +7 -exec rm -rf {} + &> /dev/null || true
    
    # Rotate logs
    find logs -name "*.log" -size +100M -exec truncate -s 0 {} \; &> /dev/null || true
    
    success "Cleanup completed"
}

# Show status
show_status() {
    log "Deployment status:"
    echo ""
    docker-compose -f "$COMPOSE_FILE" ps
    echo ""
    log "Application URL: https://$(hostname -I | awk '{print $1}')"
    log "Swagger UI: https://$(hostname -I | awk '{print $1}')/api/swagger-ui.html"
}

# Main deployment function
main() {
    log "Starting deployment of ActivityForecast Backend..."
    
    create_directories
    check_prerequisites
    create_ssl_certificates
    backup_current_deployment
    stop_services
    pull_images
    start_services
    health_check
    cleanup
    show_status
    
    success "Deployment completed successfully!"
}

# Handle script interruption
trap 'error "Deployment interrupted"' INT TERM

# Run main function
main "$@"