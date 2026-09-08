@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\install_character_update.ps1"
if errorlevel 1 (
    echo Please exit the running pet from its tray menu, then start again.
    pause
    exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0tools\start_ollama.ps1"
start "" /MIN C:\PROGRA~1\Java\jre1.8.0_441\bin\javaw.exe -Dfile.encoding=UTF-8 -jar dist\Shimeji.jar
exit
