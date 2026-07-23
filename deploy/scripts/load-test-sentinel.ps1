param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Requests = 60,
    [int]$TimeoutSec = 20,
    [int]$MinimumRateLimited = 1,
    [string]$UserAccount = "demo@refine.local",
    [string]$UserPassword = "RefineDemo123"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

if ($Requests -lt 2) {
    throw "Requests must be at least 2."
}

[System.Net.ServicePointManager]::DefaultConnectionLimit = [Math]::Max(100, $Requests)
$handler = New-Object System.Net.Http.HttpClientHandler
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)
$endpoint = "$($BaseUrl.TrimEnd('/'))/api/userAccount/login"
$payload = @{ userAccount = $UserAccount; userPassword = $UserPassword } | ConvertTo-Json -Compress
$tasks = New-Object 'System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]'
$requestMessages = New-Object 'System.Collections.Generic.List[System.Net.Http.HttpRequestMessage]'

try {
    for ($index = 0; $index -lt $Requests; $index++) {
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $endpoint)
        $request.Content = New-Object System.Net.Http.StringContent($payload, [System.Text.Encoding]::UTF8, "application/json")
        $requestMessages.Add($request)
        $tasks.Add($client.SendAsync($request))
    }

    $completed = [System.Threading.Tasks.Task]::WaitAll($tasks.ToArray(), $TimeoutSec * 1000)
    if (-not $completed) {
        throw "Sentinel load test timed out after $TimeoutSec seconds."
    }

    $statusCounts = @{}
    $rateLimited = 0
    $invalidRateLimitBodies = 0
    foreach ($task in $tasks) {
            $response = $task.Result
        try {
            $status = [int]$response.StatusCode
            $statusKey = $status.ToString()
            if (-not $statusCounts.ContainsKey($statusKey)) {
                $statusCounts[$statusKey] = 0
            }
            $statusCounts[$statusKey]++
            if ($status -eq 429) {
                $rateLimited++
                $body = $response.Content.ReadAsStringAsync().Result
                try {
                    $errorResponse = $body | ConvertFrom-Json
                    if ([int]$errorResponse.code -ne 429) {
                        $invalidRateLimitBodies++
                    }
                } catch {
                    $invalidRateLimitBodies++
                }
            }
        } finally {
            $response.Dispose()
        }
    }

    $summary = [ordered]@{
        endpoint = $endpoint
        requests = $Requests
        statusCounts = $statusCounts
        rateLimited = $rateLimited
        minimumExpected = $MinimumRateLimited
        invalidRateLimitBodies = $invalidRateLimitBodies
    }
    $summary | ConvertTo-Json -Depth 4

    if ($rateLimited -lt $MinimumRateLimited) {
        throw "Expected at least $MinimumRateLimited HTTP 429 responses, but received $rateLimited."
    }
    if ($invalidRateLimitBodies -gt 0) {
        throw "$invalidRateLimitBodies HTTP 429 responses did not use the expected JSON code 429 contract."
    }
    if (-not $statusCounts.ContainsKey("200")) {
        throw "No login request passed through the limiter; check the route and downstream service."
    }

    Write-Host "PASS: Sentinel admitted requests and rate-limited excess traffic with the compatible JSON response."
} finally {
    foreach ($request in $requestMessages) {
        $request.Dispose()
    }
    $client.Dispose()
    $handler.Dispose()
}
