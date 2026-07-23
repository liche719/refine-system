param(
    [string]$EnvFile = ".env",
    [string]$ComposeProject = "refine-microservices"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $ProjectRoot
try {
    $envValues = @{}
    Get-Content $EnvFile | Where-Object { $_ -match '^[^#].*=.*$' } | ForEach-Object {
        $key, $value = $_ -split '=', 2
        $envValues[$key] = $value
    }
    $checks = @(
        @{ User = "identity_app"; Password = $envValues.IDENTITY_DB_PASSWORD; Own = "identity_db"; Foreign = "learning_db" },
        @{ User = "learning_app"; Password = $envValues.LEARNING_DB_PASSWORD; Own = "learning_db"; Foreign = "ai_db" },
        @{ User = "ai_app"; Password = $envValues.AI_DB_PASSWORD; Own = "ai_db"; Foreign = "identity_db" }
    )
    foreach ($check in $checks) {
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.infra.yml exec -T mysql-primary `
            mysql "--user=$($check.User)" "--password=$($check.Password)" "--execute=SHOW TABLES FROM $($check.Own);" 2>$null | Out-Null
        $ownExitCode = $LASTEXITCODE
        docker compose -p $ComposeProject --env-file $EnvFile -f docker-compose.infra.yml exec -T mysql-primary `
            mysql "--user=$($check.User)" "--password=$($check.Password)" "--execute=SHOW TABLES FROM $($check.Foreign);" 2>$null | Out-Null
        $foreignExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousPreference

        if ($ownExitCode -ne 0) {
            throw "$($check.User) could not access its own schema $($check.Own)"
        }
        if ($foreignExitCode -eq 0) {
            throw "$($check.User) unexpectedly accessed $($check.Foreign)"
        }
        Write-Host "$($check.User): own schema OK, foreign schema denied"
    }
} finally {
    Pop-Location
}
