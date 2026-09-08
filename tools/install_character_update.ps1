$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$staged = Join-Path $projectRoot 'dist/.updates/Shimeji.jar'
$target = Join-Path $projectRoot 'dist/Shimeji.jar'
if (-not (Test-Path -LiteralPath $staged)) { exit 0 }
try {
    # Open exclusively first: do not touch the installed JAR while a pet holds it open.
    $stream = [System.IO.File]::Open($target, 'Open', 'ReadWrite', 'None')
    $stream.Close()
    [System.IO.File]::Replace($staged, $target, [NullString]::Value)
    Write-Host 'Character update installed.'
    exit 0
} catch {
    Write-Host ('Update not installed: ' + $_.Exception.Message)
    Write-Host 'If the pet is running, exit it first, then retry.'
    exit 1
}
