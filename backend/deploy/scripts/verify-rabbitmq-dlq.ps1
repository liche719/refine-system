param(
    [string]$EnvFile = ".env",
    [string]$ManagementBaseUrl = "http://localhost:15672"
)

$ErrorActionPreference = "Stop"

function Get-EnvValue {
    param(
        [string]$Path,
        [string]$Name
    )

    $line = Get-Content $Path | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -First 1
    if (-not $line) {
        throw "$Name is missing from $Path"
    }

    return ($line -split '=', 2)[1]
}

function Invoke-RabbitManagement {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [hashtable]$Headers
    )

    $request = @{
        Uri = "$ManagementBaseUrl$Path"
        Method = $Method
        Headers = $Headers
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $request.ContentType = "application/json"
        $request.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }

    return Invoke-RestMethod @request
}

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Push-Location $projectRoot
try {
    $username = "refine"
    if (Get-Content $EnvFile | Where-Object { $_ -match '^RABBITMQ_USERNAME=' } | Select-Object -First 1) {
        $username = Get-EnvValue -Path $EnvFile -Name "RABBITMQ_USERNAME"
    }
    $password = Get-EnvValue -Path $EnvFile -Name "RABBITMQ_PASSWORD"
    $credentialBytes = [System.Text.Encoding]::UTF8.GetBytes("${username}:${password}")
    $headers = @{ Authorization = "Basic $([Convert]::ToBase64String($credentialBytes))" }

    $suffix = [Guid]::NewGuid().ToString("N")
    $sourceQueue = "refine.verify.dlq.source.$suffix"
    $deadLetterQueue = "refine.verify.dlq.target.$suffix"
    $deadLetterExchange = "refine.verify.dlq.exchange.$suffix"
    $routingKey = "refine.verify.dlq.$suffix"
    $verificationPayload = "refine-dlq-verification-$suffix"

    try {
        Invoke-RabbitManagement -Method "Put" -Path "/api/exchanges/%2F/$deadLetterExchange" -Headers $headers -Body @{
            type = "direct"; durable = $false; auto_delete = $false; internal = $false; arguments = @{}
        } | Out-Null
        Invoke-RabbitManagement -Method "Put" -Path "/api/queues/%2F/$deadLetterQueue" -Headers $headers -Body @{
            durable = $false; auto_delete = $false; arguments = @{}
        } | Out-Null
        Invoke-RabbitManagement -Method "Post" -Path "/api/bindings/%2F/e/$deadLetterExchange/q/$deadLetterQueue" -Headers $headers -Body @{
            routing_key = $routingKey; arguments = @{}
        } | Out-Null
        Invoke-RabbitManagement -Method "Put" -Path "/api/queues/%2F/$sourceQueue" -Headers $headers -Body @{
            durable = $false; auto_delete = $false; arguments = @{
                "x-dead-letter-exchange" = $deadLetterExchange
                "x-dead-letter-routing-key" = $routingKey
            }
        } | Out-Null

        $publishResult = Invoke-RabbitManagement -Method "Post" -Path "/api/exchanges/%2F/amq.default/publish" -Headers $headers -Body @{
            properties = @{}
            routing_key = $sourceQueue
            payload = $verificationPayload
            payload_encoding = "string"
        }
        if (-not $publishResult.routed) {
            throw "The verification message was not routed to the source queue."
        }

        $rejected = Invoke-RabbitManagement -Method "Post" -Path "/api/queues/%2F/$sourceQueue/get" -Headers $headers -Body @{
            count = 1; ackmode = "reject_requeue_false"; encoding = "auto"; truncate = 50000
        }
        if (@($rejected).Count -ne 1) {
            throw "Expected one source message to reject, received $(@($rejected).Count)."
        }

        $deadLettered = $null
        for ($attempt = 1; $attempt -le 20; $attempt++) {
            Start-Sleep -Milliseconds 100
            $candidate = @(Invoke-RabbitManagement -Method "Post" -Path "/api/queues/%2F/$deadLetterQueue/get" -Headers $headers -Body @{
                count = 1; ackmode = "ack_requeue_true"; encoding = "auto"; truncate = 50000
            })
            if ($candidate.Count -eq 1 -and $candidate[0].payload -eq $verificationPayload) {
                $deadLettered = $candidate[0]
                break
            }
        }
        if ($null -eq $deadLettered) {
            throw "Expected the rejected verification message to reach the dead-letter queue."
        }

        Write-Host "PASS: RabbitMQ routed a rejected message into a dead-letter queue."
    } finally {
        foreach ($queue in @($sourceQueue, $deadLetterQueue)) {
            try {
                Invoke-RabbitManagement -Method "Delete" -Path "/api/queues/%2F/$queue" -Headers $headers -Body $null | Out-Null
            } catch {
                Write-Warning "Could not delete temporary queue ${queue}: $($_.Exception.Message)"
            }
        }
        try {
            Invoke-RabbitManagement -Method "Delete" -Path "/api/exchanges/%2F/$deadLetterExchange" -Headers $headers -Body $null | Out-Null
        } catch {
            Write-Warning "Could not delete temporary exchange ${deadLetterExchange}: $($_.Exception.Message)"
        }
    }
} finally {
    Pop-Location
}
