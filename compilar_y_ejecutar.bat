@echo off
chcp 65001 > nul
cd /d "%~dp0"

echo ========================================================
echo   COMPILANDO Y EJECUTANDO AJEDREZ (HEXAGONAL + SWING)
echo ========================================================

REM --- 1. Java (obligatorio) ---
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] No se encontro Java JDK ^(javac^). Instalalo desde https://adoptium.net/ y volve a intentar.
    pause
    exit /b 1
)

REM --- 2. Dependencias de la IA: Python + python-chess (opcional) ---
echo Verificando dependencias de la IA...
set "PY="
for %%P in (python py python3) do (
    if not defined PY (
        %%P -c "import sys" >nul 2>nul && set "PY=%%P"
    )
)
if not defined PY (
    echo [AVISO] No se encontro Python. El modo "Contra la IA" no va a estar disponible.
    echo         Instalalo desde https://www.python.org/downloads/ y volve a ejecutar este archivo.
    goto compilar
)
%PY% -c "import chess" >nul 2>nul
if not errorlevel 1 (
    echo python-chess ya esta instalado.
    goto compilar
)
echo Instalando python-chess ^(libreria de la IA^)...
%PY% -m pip install --user --disable-pip-version-check -q -r ia\requirements.txt
%PY% -c "import chess" >nul 2>nul
if errorlevel 1 (
    echo [AVISO] No se pudo instalar python-chess. El modo "Contra la IA" no va a estar disponible.
    echo         Podes instalarlo a mano con: %PY% -m pip install -r ia\requirements.txt
) else (
    echo python-chess instalado correctamente.
)

:compilar
REM --- 3. Compilar ---
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

REM --- 4. Ejecutar ---
echo Compilacion exitosa. Iniciando juego...
java -cp bin ajedrez.adaptadores.ui.swing.Main
pause
