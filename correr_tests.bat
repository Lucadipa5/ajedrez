@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo ========================================================
echo   COMPILANDO Y EJECUTANDO PRUEBAS UNITARIAS DEL NUCLEO
echo ========================================================

if not exist bin mkdir bin

echo Compilando codigo fuente y tests...
dir /s /b "src\*.java" > fuentes.txt 2>nul
javac -encoding UTF-8 -d bin @fuentes.txt
set RESULT=%errorlevel%
del /f /q fuentes.txt 2>nul

if %RESULT% neq 0 (
    echo [ERROR] Hubo un error de compilacion.
    pause
    exit /b %RESULT%
)

echo Ejecutando Suite de Tests...
java -cp bin ajedrez.tests.TestRunner
pause
