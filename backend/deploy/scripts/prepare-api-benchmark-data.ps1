param(
    [switch]$Cleanup
)

$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\..\.env"
$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"
$sqlName = if ($Cleanup) { "cleanup-benchmark-data.sql" } else { "prepare-benchmark-data.sql" }
$sqlFile = Join-Path $PSScriptRoot $sqlName

if (-not (Test-Path -LiteralPath $envFile)) {
    throw ".env was not found. Start the local Compose environment before preparing benchmark data."
}

$rootPasswordLine = Get-Content -LiteralPath $envFile |
    Where-Object { $_ -match "^MYSQL_ROOT_PASSWORD=" } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($rootPasswordLine)) {
    throw "MYSQL_ROOT_PASSWORD was not found in .env."
}
$rootPassword = $rootPasswordLine.Substring("MYSQL_ROOT_PASSWORD=".Length)

Get-Content -Raw -LiteralPath $sqlFile |
    docker compose -p refine-microservices --env-file $envFile -f $composeFile exec -T `
        -e "MYSQL_PWD=$rootPassword" mysql-primary mysql -uroot

if ($LASTEXITCODE -ne 0) {
    throw "Benchmark fixture command failed with exit code $LASTEXITCODE."
}

if ($Cleanup) {
    Write-Host "Removed local benchmark fixture rows."
} else {
    Write-Host "Prepared 20,000 local benchmark fixture rows. Wait for replication before testing reads."
}
