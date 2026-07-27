param(
    [string]$BeforePattern = ".runtime\benchmarks\before-index-*.json",
    [string]$AfterPattern = ".runtime\benchmarks\after-index-*.json"
)

$ErrorActionPreference = "Stop"

function Read-BenchmarkResults([string]$pattern) {
    $files = Get-ChildItem -Path $pattern -File | Sort-Object Name
    if ($files.Count -lt 1) {
        throw "No benchmark result matched: $pattern"
    }
    return @($files | ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName | ConvertFrom-Json })
}

function Get-Median([double[]]$values) {
    $sorted = @($values | Sort-Object)
    return [double]$sorted[[int][Math]::Floor($sorted.Count / 2)]
}

function Get-MetricValues($results, [string]$metric) {
    return @($results | ForEach-Object {
        $value = $_
        foreach ($part in $metric.Split('.')) {
            $value = $value.$part
        }
        [double]$value
    })
}

$before = Read-BenchmarkResults $BeforePattern
$after = Read-BenchmarkResults $AfterPattern
$metrics = @("throughputRps", "latencyMs.p50", "latencyMs.p95", "latencyMs.p99", "latencyMs.average")

$summary = foreach ($metric in $metrics) {
    $beforeMedian = Get-Median (Get-MetricValues $before $metric)
    $afterMedian = Get-Median (Get-MetricValues $after $metric)
    [PSCustomObject]@{
        Metric = $metric
        BeforeMedian = [Math]::Round($beforeMedian, 2)
        AfterMedian = [Math]::Round($afterMedian, 2)
        ChangePercent = [Math]::Round((($afterMedian - $beforeMedian) / $beforeMedian) * 100, 2)
    }
}

$beforeFailures = ($before | Measure-Object -Property failedRequests -Sum).Sum
$afterFailures = ($after | Measure-Object -Property failedRequests -Sum).Sum

$summary | Format-Table -AutoSize
Write-Host "Before failures: $beforeFailures"
Write-Host "After failures: $afterFailures"
