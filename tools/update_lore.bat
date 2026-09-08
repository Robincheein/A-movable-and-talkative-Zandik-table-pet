@echo off
chcp 65001 >nul
cd /d "%~dp0.."
set PYTHONIOENCODING=utf-8
call conda run --no-capture-output -n steel python tools/update_lore.py --proxy direct %*
if errorlevel 1 echo 部分资料未更新，请查看 knowledge/last_run.json。
pause
