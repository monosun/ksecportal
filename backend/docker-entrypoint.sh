#!/bin/sh
# Fix bind-mount directories that may have been created by root in previous runs
mkdir -p /app/uploads /app/backups
chown -R appuser:appgroup /app/uploads /app/backups 2>/dev/null || true
exec su-exec appuser java -jar /app/app.jar "$@"
