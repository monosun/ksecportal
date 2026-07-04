# SecPortal Server Stop Script (Windows PowerShell)

param(
    [switch]$Clean  # Also remove DB volumes (wipes all data)
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host "=== SecPortal Server Stopping ===" -ForegroundColor Cyan

Set-Location $ProjectRoot

if ($Clean) {
    Write-Host "[WARN] This will remove containers AND DB volumes (all data will be lost)..." -ForegroundColor Yellow
    $Confirm = Read-Host "Are you sure? (y/N)"
    if ($Confirm -ne "y" -and $Confirm -ne "Y") {
        Write-Host "[INFO] Cancelled." -ForegroundColor Gray
        exit 0
    }
    docker compose down -v --remove-orphans
} else {
    docker compose down
}

Write-Host "[INFO] Server stopped." -ForegroundColor Green
