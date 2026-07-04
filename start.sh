#!/usr/bin/env bash
# SecPortal Server Start Script (Linux/macOS)

set -e

BUILD=false
CLEAN=false
LOGS=false

usage() {
    echo "Usage: $0 [--build] [--clean] [--logs]"
    echo "  --build   Force image rebuild"
    echo "  --clean   Full reset including DB volumes, then start"
    echo "  --logs    Stream logs after start"
    exit 1
}

for arg in "$@"; do
    case $arg in
        --build) BUILD=true ;;
        --clean) CLEAN=true ;;
        --logs)  LOGS=true  ;;
        *)       usage      ;;
    esac
done

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo "=== SecPortal Server Starting ==="

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "[ERROR] Docker is not running. Please start Docker first."
    exit 1
fi

# Check / create .env file
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "[WARN] .env not found. Copied from .env.example."
        echo "       In production, make sure to change JWT_SECRET and other secrets."
    else
        echo "[ERROR] Neither .env nor .env.example was found."
        exit 1
    fi
fi

# Full reset (--clean flag)
if [ "$CLEAN" = true ]; then
    echo "[INFO] Removing existing containers and DB volumes..."
    docker compose down -v --remove-orphans 2>/dev/null || true
fi

# Start services
COMPOSE_ARGS="up -d"
if [ "$BUILD" = true ]; then
    COMPOSE_ARGS="$COMPOSE_ARGS --build"
fi

echo "[INFO] Starting containers..."
docker compose $COMPOSE_ARGS

# Wait for services to be ready (up to 60 seconds)
echo "[INFO] Checking service readiness..."
MAX_WAIT=60
ELAPSED=0
INTERVAL=5

while [ $ELAPSED -lt $MAX_WAIT ]; do
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))

    BACKEND_STATUS=$(docker inspect --format "{{.State.Status}}" secportal-backend 2>/dev/null || echo "unknown")
    DB_STATUS=$(docker inspect --format "{{.State.Health.Status}}" secportal-db 2>/dev/null || echo "unknown")

    echo "  DB: $DB_STATUS | Backend: $BACKEND_STATUS (${ELAPSED}/${MAX_WAIT}s)"

    if [ "$BACKEND_STATUS" = "running" ] && [ "$DB_STATUS" = "healthy" ]; then
        break
    fi
done

echo ""
echo "=== Status ==="
docker compose ps

echo ""
echo "=== Access Info ==="
echo "  Frontend     : http://localhost"
echo "  Backend API  : http://localhost:8080/api"
echo "  Default Admin: secportal@monosun.com / Ksecurity!!!"
echo ""

if [ "$LOGS" = true ]; then
    echo "[INFO] Streaming logs. (Press Ctrl+C to stop)"
    docker compose logs -f
fi
