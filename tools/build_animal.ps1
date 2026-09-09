$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jdk = 'C:\Program Files\Java\jdk-1.8\bin'
if (-not (Test-Path "$jdk/javac.exe") -and $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    $jdk = Join-Path $env:JAVA_HOME 'bin'
}
$ErrorActionPreference = 'Continue'
$jdkVersion = & "$jdk/java.exe" -version 2>&1 | Out-String
$ErrorActionPreference = 'Stop'
if ($jdkVersion -notmatch '"1\.8\.') { throw 'This Shimeji engine requires JDK 8 (bundled Nashorn). Set JAVA_HOME to JDK 8.' }
$classes = Join-Path $projectRoot 'build/animal_classes'
$tests = Join-Path $projectRoot 'build/animal_tests'
# Match manifest.mf exactly: Java 8 already has Nashorn. The optional newer
# nashorn-core JAR in lib is Java 11+ and must not enter the Java 8 runtime path.
$runtimeLibs = 'lib/jna.jar;lib/platform.jar;lib/gson-2.10.1.jar;lib/sqlite-jdbc-3.42.0.0.jar'
New-Item -ItemType Directory -Path $classes,$tests,'dist/.updates' -Force | Out-Null
$sources = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk/javac.exe" -source 8 -target 8 -encoding UTF-8 -cp 'lib/*' -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compile failed' }
& "$jdk/javac.exe" -source 8 -target 8 -encoding UTF-8 -cp "$classes;lib/*" -d $tests tests/AnimalSkinSelfTest.java tests/CharacterPromptSelfTest.java tests/WindowFrameSelfTest.java tests/PetMoodSelfTest.java tests/ScreenBoundsSelfTest.java
if ($LASTEXITCODE -ne 0) { throw 'Test compile failed' }
Copy-Item img/*.png -Destination dist/img -Force
Copy-Item conf/Actions.xml,conf/Behavior.xml,conf/chat_config.example.properties -Destination dist/conf -Force
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;$classes;img;conf;$runtimeLibs" com.group_finity.mascot.AnimalSkinSelfTest .
if ($LASTEXITCODE -ne 0) { throw 'Skin test failed' }
# Build only application classes, never private config/data or unrelated test classes.
& "$jdk/jar.exe" cfm build/animal_candidate.jar manifest.mf -C $classes com
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;build/animal_candidate.jar;$runtimeLibs" com.group_finity.mascot.PetMoodSelfTest
if ($LASTEXITCODE -ne 0) { throw 'Pet mood regression failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;build/animal_candidate.jar;$runtimeLibs" com.group_finity.mascot.ScreenBoundsSelfTest
if ($LASTEXITCODE -ne 0) { throw 'Screen bounds regression failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;build/animal_candidate.jar;dist/img;dist/conf;$runtimeLibs" com.group_finity.mascot.AnimalSkinSelfTest dist
if ($LASTEXITCODE -ne 0) { throw 'Packaged skin test failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;build/animal_candidate.jar;$runtimeLibs" com.group_finity.mascot.win.WindowFrameSelfTest
if ($LASTEXITCODE -ne 0) { throw 'Window frame regression failed' }
& "$jdk/java.exe" '-Dfile.encoding=UTF-8' -cp "$tests;build/animal_candidate.jar;$runtimeLibs" com.group_finity.mascot.chat.CharacterPromptSelfTest .
if ($LASTEXITCODE -ne 0) { throw 'Prompt regression failed' }
Copy-Item build/animal_candidate.jar -Destination dist/.updates/Shimeji.jar -Force
try {
    [IO.File]::Replace((Join-Path $projectRoot 'dist/.updates/Shimeji.jar'), (Join-Path $projectRoot 'dist/Shimeji.jar'), [NullString]::Value)
    Write-Host 'ANIMAL_BUILD_OK: dist/Shimeji.jar'
} catch {
    Write-Host ('ANIMAL_BUILD_STAGED: installation deferred: ' + $_.Exception.Message)
}
