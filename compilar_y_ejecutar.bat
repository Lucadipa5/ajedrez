@echo off
chcp 65001 > nul
cd /d "%~dp0"

echo ========================================================
echo   COMPILANDO Y EJECUTANDO AJEDREZ (HEXAGONAL + SWING)
echo ========================================================

if not exist bin mkdir bin
if exist fuentes.txt del /f /q fuentes.txt

echo Buscando archivos fuente...
dir /s /b "src\*.java" > fuentes.txt 2>nul

for %%A in (fuentes.txt) do if %%~zA==0 (
    del /f /q fuentes.txt 2>nul
    echo [ERROR] No se encontraron archivos .java en src\
    pause
    exit /b 1
)

echo Compilando codigo fuente...
javac -encoding UTF-8 -d bin @fuentes.txt
set RESULT=%errorlevel%

del /f /q fuentes.txt 2>nul

if %RESULT% neq 0 (
    echo [ERROR] Error de compilacion. Verifique que Java JDK este instalado.
    pause
    exit /b %RESULT%
)

echo Copiando recursos...
if exist "src\ajedrez\adaptadores\ui\swing\recursos" (
    if not exist "bin\ajedrez\adaptadores\ui\swing\recursos" mkdir "bin\ajedrez\adaptadores\ui\swing\recursos"
    xcopy /Y /Q "src\ajedrez\adaptadores\ui\swing\recursos\*.*" "bin\ajedrez\adaptadores\ui\swing\recursos\" >nul 2>nul
)

echo Compilacion exitosa. Iniciando juego...
java -cp bin ajedrez.adaptadores.ui.swing.Main
pause
