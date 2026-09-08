$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jdk = 'C:\Program Files\Java\jdk-1.8\bin'
if (-not (Test-Path -LiteralPath "$jdk\javac.exe")) { throw 'Java 8 JDK not found.' }
$classes = Join-Path $projectRoot 'build/character_update'
$testClasses = Join-Path $projectRoot 'build/character_tests'
New-Item -ItemType Directory -Path $classes,$testClasses -Force | Out-Null
$javaSources = Get-ChildItem -LiteralPath src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" -source 8 -target 8 -encoding UTF-8 -cp 'lib/*' -d $classes $javaSources
if ($LASTEXITCODE -ne 0) { throw 'Java compile failed.' }
& "$jdk\javac.exe" -source 8 -target 8 -encoding UTF-8 -cp "$classes;lib/*" -d $testClasses tests/RebuildRagIndex.java tests/RagIndexSelfTest.java tests/CharacterPromptSelfTest.java tests/CharacterLivePreview.java
if ($LASTEXITCODE -ne 0) { throw 'Test compile failed.' }
& "$jdk\java.exe" '-Dfile.encoding=UTF-8' -cp "$testClasses;$classes;lib/*" com.group_finity.mascot.chat.CharacterPromptSelfTest .
if ($LASTEXITCODE -ne 0) { throw 'Prompt integration test failed.' }
# Synchronize character documents only. Never copy or replace conversation databases/profiles.
$characterRoot = (Resolve-Path -LiteralPath character).Path
Get-ChildItem -LiteralPath $characterRoot -Recurse -Filter *.txt |
    Where-Object { $_.FullName -notmatch '\\memory\\' } |
    ForEach-Object {
        $relative = $_.FullName.Substring($characterRoot.Length + 1)
        $destination = Join-Path 'dist/character' $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
    }
foreach ($base in @('.', 'dist')) {
    & "$jdk\java.exe" '-Dfile.encoding=UTF-8' -cp "$testClasses;$classes;lib/*" com.group_finity.mascot.chat.RebuildRagIndex $base
    if ($LASTEXITCODE -ne 0) { throw 'Index build failed.' }
    & "$jdk\java.exe" '-Dfile.encoding=UTF-8' -cp "$testClasses;$classes;lib/*" com.group_finity.mascot.chat.RagIndexSelfTest $base
    if ($LASTEXITCODE -ne 0) { throw 'Index verification failed.' }
}
$backup = Join-Path $projectRoot 'docs/character_before_20260907/Shimeji.jar'
if (-not (Test-Path -LiteralPath $backup)) {
    New-Item -ItemType Directory -Path (Split-Path -Parent $backup) -Force | Out-Null
    Copy-Item -LiteralPath 'dist/Shimeji.jar' -Destination $backup
}
# Build a staged JAR so a running pet never receives a partial archive.
$staged = Join-Path $projectRoot 'dist/.updates/Shimeji.jar'
New-Item -ItemType Directory -Path (Split-Path -Parent $staged) -Force | Out-Null
Copy-Item -LiteralPath 'dist/Shimeji.jar' -Destination $staged -Force
& "$jdk\jar.exe" uf $staged -C $classes com
if ($LASTEXITCODE -ne 0) { throw 'Staged JAR build failed.' }
& "$jdk\java.exe" '-Dfile.encoding=UTF-8' -cp "$testClasses;$staged;lib/*" com.group_finity.mascot.chat.CharacterPromptSelfTest dist
if ($LASTEXITCODE -ne 0) { throw 'Packaged JAR verification failed.' }
try {
    [System.IO.File]::Replace($staged, (Join-Path $projectRoot 'dist/Shimeji.jar'), [NullString]::Value)
    Write-Host 'BUILD_CHARACTER_OK: dist/Shimeji.jar'
} catch {
    Write-Host 'BUILD_CHARACTER_STAGED: close the pet and use the root startup BAT to install.'
}
