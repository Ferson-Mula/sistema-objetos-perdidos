@echo off
chcp 65001 >nul
call "%~dp0compilar.bat"
java -Dfile.encoding=UTF-8 -cp out TestesSistema
