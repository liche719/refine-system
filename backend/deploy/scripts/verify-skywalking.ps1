param(
    [string[]]$ExpectedServices = @(
        "refine-gateway",
        "refine-identity-service",
        "refine-learning-service",
        "refine-ai-service"
    )
)

$ErrorActionPreference = "Stop"
$body = @{
    query = @"
query queryServices(`$layer: String!) {
  services: listServices(layer: `$layer) {
    name
    layers
    normal
  }
}
"@
    variables = @{layer = "GENERAL"}
} | ConvertTo-Json -Depth 5

$response = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:12800/graphql" `
    -ContentType "application/json" `
    -Body $body `
    -TimeoutSec 15

if ($response.errors) {
    throw "SkyWalking GraphQL error: $($response.errors | ConvertTo-Json -Compress)"
}

$reported = @($response.data.services | ForEach-Object { $_.name })
$missing = @($ExpectedServices | Where-Object { $_ -notin $reported })
if ($missing.Count -gt 0) {
    throw "SkyWalking has not received traces for: $($missing -join ', '). Send smoke traffic through Gateway and retry."
}

Write-Host "SkyWalking reports all Refine services: $($ExpectedServices -join ', ')"
