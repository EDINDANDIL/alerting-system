@echo off
REM Запуск CLI клиента Alerting System
REM Использование: run-client.bat

cd /d "%~dp0"

echo ╔══════════════════════════════════════════════════════════════╗
echo ║          Alerting System - CLI Client                        ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

REM Проверка наличия Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java не найдена. Установите JDK 25.
    exit /b 1
)

echo Запуск клиента...
echo.
echo Подсказка: введите 'help' для списка команд
echo.

REM Запуск fat JAR
java -jar client-desktop\build\libs\client-desktop-1.0-SNAPSHOT-fat.jar

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Ошибка запуска. Проверьте версию Java (требуется Java 25).
    echo Для выхода нажмите любую клавишу...
    pause >nul
)
