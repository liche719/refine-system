param(
    [string]$EnvFile = ".env",
    [string]$ComposeProject = "refine-microservices"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $ProjectRoot
try {
    $rootPassword = ((Get-Content $EnvFile | Where-Object { $_ -match '^MYSQL_ROOT_PASSWORD=' }) -split '=', 2)[1]
    if (-not $rootPassword) {
        throw "MYSQL_ROOT_PASSWORD is missing from $EnvFile"
    }

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.infra.yml exec -T mysql-replica `
        mysql -uroot "--password=$rootPassword" '--execute=SHOW REPLICA STATUS;'
    $statusExitCode = $LASTEXITCODE
    docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.infra.yml exec -T mysql-replica `
        mysql -N -B -uroot "--password=$rootPassword" '--execute=SELECT CHANNEL_NAME,SERVICE_STATE,LAST_ERROR_NUMBER,LAST_ERROR_MESSAGE FROM performance_schema.replication_applier_status_by_worker;'
    $workerExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference

    if ($statusExitCode -ne 0 -or $workerExitCode -ne 0) {
        throw "Replication status check failed (status=$statusExitCode, workers=$workerExitCode)"
    }
} finally {
    Pop-Location
}
