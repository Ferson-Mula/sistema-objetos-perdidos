@echo off
cd /d "%~dp0.."
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java tests\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
del fontes.txt
echo Compilado.
