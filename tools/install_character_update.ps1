$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$staged = Join-Path $projectRoot 'dist/.updates/Shimeji.jar'
$target = Join-Path $projectRoot 'dist/Shimeji.jar'
if (-not (Test-Path -LiteralPath $staged)) { exit 0 }
try {
    # Open exclusively first: do not touch the installed JAR while a pet holds it open.
    $stream = [System.IO.File]::Open($target, 'Open', 'ReadWrite', 'None')
    $stream.Close()
    [System.IO.File]::Replace($staged, $target, $null)
    Write-Host 'Character update installed.'
    exit 0
} catch {
    Write-Host 'Update is ready but the installed JAR is still in use. Exit the pet first.'
    exit 1
}
