$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $projectRoot "conf\chat_config.properties"
if (Test-Path -LiteralPath $configPath) {
    $providerLine = Get-Content -LiteralPath $configPath -Encoding UTF8 |
            Where-Object { $_ -match '^\s*ai\.provider\s*=' } | Select-Object -First 1
    if ($providerLine -and $providerLine -notmatch '^\s*ai\.provider\s*=\s*ollama\s*$') {
        exit 0
    }
}

$ollamaExe = "D:\soft_system\Ollama\ollama.exe"
$healthUrl = "http://127.0.0.1:11434/api/tags"

function Test-OllamaReady {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -Proxy $null -TimeoutSec 2
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

if (Test-OllamaReady) {
    exit 0
}

if (-not (Test-Path -LiteralPath $ollamaExe)) {
    exit 2
}

Start-Process -FilePath $ollamaExe -ArgumentList "serve" -WindowStyle Hidden

for ($attempt = 0; $attempt -lt 20; $attempt++) {
    Start-Sleep -Milliseconds 500
    if (Test-OllamaReady) {
        exit 0
    }
}

exit 3
