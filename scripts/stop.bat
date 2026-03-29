@echo off
REM Скрипт для остановки всей инфраструктуры alerting-system
REM Использование: scripts\stop.bat

echo ╔══════════════════════════════════════════════════════════════╗
echo ║          Alerting System - Остановка инфраструктуры         ║
echo ╚══════════════════════════════════════════════════════════════╝

REM Проверка наличия Docker Compose
where docker compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose не найден.
    exit /b 1
)

echo Остановка всех сервисов...
docker compose down

echo.
echo Инфраструктура остановлена!
echo.
echo Для очистки данных выполните: docker compose down -v
