param(
    [ValidateSet("mistake-list", "overview", "knowledge-points", "login")]
    [string]$Scenario = "mistake-list",
    [int]$Warmup = 100,
    [int]$Requests = 1000,
    [int]$Concurrency = 20,
    [int]$TimeoutSeconds = 15,
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$UserAccount = "demo@refine.local",
    [string]$UserPassword = "RefineDemo123",
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

if ($Warmup -lt 0 -or $Requests -lt 1 -or $Concurrency -lt 1 -or $TimeoutSeconds -lt 1) {
    throw "Warmup must be zero or greater. Requests, concurrency, and timeout must be positive."
}

$java = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    "java"
}
$source = Join-Path $PSScriptRoot "ApiBenchmark.java"
if (-not (Test-Path -LiteralPath $source)) {
    throw "ApiBenchmark.java was not found beside this script."
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $runtimeDirectory = Join-Path $PSScriptRoot "..\..\.runtime\benchmarks"
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputPath = Join-Path $runtimeDirectory "$Scenario-$timestamp.json"
}

& $java $source `
    --base-url $BaseUrl `
    --scenario $Scenario `
    --warmup $Warmup `
    --requests $Requests `
    --concurrency $Concurrency `
    --timeout-seconds $TimeoutSeconds `
    --account $UserAccount `
    --password $UserPassword `
    --output $OutputPath

if ($LASTEXITCODE -ne 0) {
    throw "API benchmark failed with exit code $LASTEXITCODE."
}

Write-Host "Saved benchmark result to $OutputPath"
