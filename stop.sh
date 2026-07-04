#!/usr/bin/env bash
# SecPortal Server Stop Script (Linux/macOS)

set -e

CLEAN=false

for arg in "$@"; do
    case $arg in
        --clean) CLEAN=true ;;
        *)
            echo "Usage: $0 [--clean]"
            echo "  --clean  Also remove DB volumes (wipes all data)"
            exit 1
            ;;
    esac
done

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

echo "=== SecPortal Server Stopping ==="

if [ "$CLEAN" = true ]; then
    echo "[WARN] This will remove containers AND DB volumes (all data will be lost)..."
    read -rp "Are you sure? (y/N) " CONFIRM
    if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
        echo "[INFO] Cancelled."
        exit 0
    fi
    docker compose down -v --remove-orphans
else
    docker compose down
fi

echo "[INFO] Server stopped."
