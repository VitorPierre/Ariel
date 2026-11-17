@echo off
chcp 65001 >nul
if not exist out mkdir out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
if errorlevel 1 (echo Erro de compilacao & pause & exit /b 1)
del fontes.txt
java -cp out;src app.Main
