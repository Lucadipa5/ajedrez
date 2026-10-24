@echo off
chcp 65001 > nul
echo ========================================================
echo   COMPILANDO Y EJECUTANDO PRUEBAS UNITARIAS DEL NUCLEO
echo ========================================================

if not exist bin mkdir bin

echo Compilando codigo fuente y tests...
javac -encoding UTF-8 -d bin src/ajedrez/nucleo/dominio/*.java src/ajedrez/nucleo/estrategias/*.java src/ajedrez/nucleo/tablero/*.java src/ajedrez/nucleo/comandos/*.java src/ajedrez/nucleo/estados/*.java src/ajedrez/nucleo/reglas/*.java src/ajedrez/nucleo/fabrica/*.java src/ajedrez/nucleo/servicio/*.java src/ajedrez/adaptadores/ui/swing/*.java src/ajedrez/adaptadores/ui/swing/perfil/*.java src/ajedrez/tests/*.java

if %errorlevel% neq 0 (
    echo [ERROR] Hubo un error de compilacion.
    pause
    exit /b %errorlevel%
)

echo Ejecutando Suite de Tests...
java -cp bin ajedrez.tests.TestRunner
pause
