@echo off
chcp 65001 >nul
cd /d "%~dp0.."
if not exist out call scripts\compilar.bat
java -Dfile.encoding=UTF-8 -cp out app.Main %*
