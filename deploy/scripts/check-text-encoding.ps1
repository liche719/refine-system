param(
    [string]$Root = (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent)
)

$ErrorActionPreference = "Stop"
$strictUtf8 = New-Object System.Text.UTF8Encoding($false, $true)
$extensions = @(".java", ".xml", ".yml", ".yaml", ".json", ".md", ".sql", ".ps1", ".sh", ".properties")
$invalidFiles = New-Object System.Collections.Generic.List[string]
$suspiciousFiles = New-Object System.Collections.Generic.List[string]
$mojibakePattern = "\uFFFD|[\uE000-\uF8FF]|\u951F\u65A4\u62F7|\u951B|\u92C6|\u9225|\u9227|\u99A3"

Get-ChildItem -LiteralPath $Root -Recurse -File |
    Where-Object {
        $_.FullName -notmatch "[\\/]target[\\/]" -and
        $_.FullName -notmatch "[\\/]\.runtime[\\/]" -and
        $extensions -contains $_.Extension.ToLowerInvariant()
    } |
    ForEach-Object {
        try {
            $text = $strictUtf8.GetString([System.IO.File]::ReadAllBytes($_.FullName))
            if ($text -match $mojibakePattern) {
                $suspiciousFiles.Add($_.FullName)
            }
        } catch {
            $invalidFiles.Add($_.FullName)
        }
    }

if ($invalidFiles.Count -gt 0 -or $suspiciousFiles.Count -gt 0) {
    if ($invalidFiles.Count -gt 0) {
        Write-Error ("Non-UTF-8 files:`n" + ($invalidFiles -join "`n"))
    }
    if ($suspiciousFiles.Count -gt 0) {
        Write-Error ("Possible mojibake files:`n" + ($suspiciousFiles -join "`n"))
    }
    exit 1
}

Write-Host "PASS: all project text files are valid UTF-8 and no common mojibake markers were found."
