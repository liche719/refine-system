param(
    [string]$EnvFile = ".env",
    [int]$SkyWalkingUiPort = 8088,
    [string]$ComposeProject = "refine-microservices",
    [int]$HttpTimeoutSec = 30,
    [int]$MaxAttempts = 3
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $ProjectRoot
try {
    docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.yml ps
    foreach ($url in @(
        "http://localhost:8080/actuator/health",
        "http://localhost:8101/actuator/health",
        "http://localhost:8102/actuator/health",
        "http://localhost:8103/actuator/health",
        "http://localhost:$SkyWalkingUiPort"
    )) {
        $healthy = $false
        for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
            try {
                Invoke-RestMethod -Uri $url -TimeoutSec $HttpTimeoutSec | Out-Null
                $healthy = $true
                break
            } catch {
                if ($attempt -eq $MaxAttempts) {
                    throw
                }
                Start-Sleep -Seconds 2
            }
        }
        if ($healthy) {
            Write-Host "UP $url"
        }
    }

    if (-not (Test-NetConnection -ComputerName localhost -Port 12800 -InformationLevel Quiet)) {
        throw "SkyWalking OAP is not listening on localhost:12800"
    }
    Write-Host "UP SkyWalking OAP localhost:12800"
} finally {
    Pop-Location
}
