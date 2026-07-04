# SecPortal Server Start Script (Windows PowerShell)

param(
    [switch]$Build,       # Force image rebuild
    [switch]$Clean,       # Full reset including DB volumes, then start
    [switch]$Logs         # Stream logs after start
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host "=== SecPortal Server Starting ===" -ForegroundColor Cyan

# Check Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "[ERROR] Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Check / create .env file
$EnvFile = Join-Path $ProjectRoot ".env"
$EnvExample = Join-Path $ProjectRoot ".env.example"

if (-not (Test-Path $EnvFile)) {
    if (Test-Path $EnvExample) {
        Copy-Item $EnvExample $EnvFile
        Write-Host "[WARN] .env not found. Copied from .env.example." -ForegroundColor Yellow
        Write-Host "       In production, make sure to change JWT_SECRET and other secrets." -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] Neither .env nor .env.example was found." -ForegroundColor Red
        exit 1
    }
}

Set-Location $ProjectRoot

# Full reset (--Clean flag)
if ($Clean) {
    Write-Host "[INFO] Removing existing containers and DB volumes..." -ForegroundColor Yellow
    docker compose down -v --remove-orphans 2>&1 | Out-Null
}

# Start services
$ComposeArgs = @("compose", "up", "-d")
if ($Build) {
    $ComposeArgs += "--build"
}

Write-Host "[INFO] Starting containers..." -ForegroundColor Green
docker @ComposeArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] docker compose up failed. Check the errors above." -ForegroundColor Red
    exit 1
}

# Wait for services to be ready (up to 60 seconds)
Write-Host "[INFO] Checking service readiness..." -ForegroundColor Green
$MaxWait = 60
$Elapsed = 0
$Interval = 5

while ($Elapsed -lt $MaxWait) {
    Start-Sleep -Seconds $Interval
    $Elapsed += $Interval

    $BackendStatus = docker inspect --format "{{.State.Status}}" secportal-backend 2>$null
    $DbStatus      = docker inspect --format "{{.State.Health.Status}}" secportal-db 2>$null

    Write-Host "  DB: $DbStatus | Backend: $BackendStatus ($Elapsed/$($MaxWait)s)" -ForegroundColor Gray

    if ($BackendStatus -eq "running" -and $DbStatus -eq "healthy") {
        break
    }
}

Write-Host ""
Write-Host "=== Status ===" -ForegroundColor Cyan
docker compose ps

Write-Host ""
Write-Host "=== Access Info ===" -ForegroundColor Cyan
Write-Host "  Frontend     : http://localhost" -ForegroundColor Green
Write-Host "  Backend API  : http://localhost:8080/api" -ForegroundColor Green
Write-Host "  Default Admin: secportal@monosun.com / Ksecurity!!!" -ForegroundColor Green
Write-Host ""

if ($Logs) {
    Write-Host "[INFO] Streaming logs. (Press Ctrl+C to stop)" -ForegroundColor Gray
    docker compose logs -f
}
