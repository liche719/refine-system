param(
    [string]$EnvFile = ".env",
    [string]$ComposeProject = "refine-microservices",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
if (-not $Force) {
    throw "Manual promotion is destructive to the current replication topology. Re-run with -Force after stopping writes to the primary."
}

$ProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $ProjectRoot
try {
    $rootPassword = ((Get-Content $EnvFile | Where-Object { $_ -match '^MYSQL_ROOT_PASSWORD=' }) -split '=', 2)[1]
    if (-not $rootPassword) {
        throw "MYSQL_ROOT_PASSWORD is missing from $EnvFile"
    }
    docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.infra.yml exec -T mysql-replica `
        mysql -uroot "--password=$rootPassword" '--execute=STOP REPLICA; RESET REPLICA ALL; SET GLOBAL super_read_only=OFF; SET GLOBAL read_only=OFF;'
    Write-Host "Replica promoted. Point MYSQL_PRIMARY_HOST to the promoted instance and restart services."
    Write-Host "This script intentionally does not reconfigure or fence the old primary."
} finally {
    Pop-Location
}
