param(
    [string]$Destination = ".runtime\skywalking"
)

$ErrorActionPreference = "Stop"
$version = "9.6.0"
$expectedSha512 = "64346286924aafcbd5e44358e4fd720a52900192bc5a32846283feee728aad90454ac8574683599b7fe4d59d587a4c1ae58744ed01028d5010847b1277906afa"
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$destinationPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Destination))
$projectRootPrefix = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
if (-not $destinationPath.StartsWith($projectRootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Destination must stay inside the project directory: $projectRoot"
}
$agentPath = Join-Path $destinationPath "agent"
$archive = Join-Path ([System.IO.Path]::GetTempPath()) "apache-skywalking-java-agent-$version.tgz"
$url = "https://archive.apache.org/dist/skywalking/java-agent/$version/apache-skywalking-java-agent-$version.tgz"

if (Test-Path (Join-Path $agentPath "skywalking-agent.jar")) {
    Write-Host "SkyWalking agent already installed at $agentPath"
    exit 0
}

New-Item -ItemType Directory -Force -Path $destinationPath | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive
$actualSha512 = (Get-FileHash -Algorithm SHA512 -LiteralPath $archive).Hash.ToLowerInvariant()
if ($actualSha512 -ne $expectedSha512) {
    throw "SkyWalking agent checksum mismatch: $actualSha512"
}

$extractPath = Join-Path $destinationPath "extract"
if (Test-Path -LiteralPath $extractPath) {
    Remove-Item -LiteralPath $extractPath -Force -Recurse
}
if (Test-Path -LiteralPath $agentPath) {
    Remove-Item -LiteralPath $agentPath -Force -Recurse
}
New-Item -ItemType Directory -Force -Path $extractPath | Out-Null
tar -xzf $archive -C $extractPath
if ($LASTEXITCODE -ne 0) {
    throw "Unable to extract SkyWalking agent archive"
}
Move-Item -LiteralPath (Join-Path $extractPath "skywalking-agent") -Destination $agentPath
Remove-Item -LiteralPath $extractPath -Force -Recurse
Remove-Item -LiteralPath $archive -Force

Write-Host "SkyWalking agent installed at $agentPath"
Write-Host "VM option: -javaagent:$agentPath\skywalking-agent.jar"
Write-Host "Environment: SW_AGENT_NAME=<service-name>; SW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800"
