@echo off
REM Скрипт для полной пересборки и запуска всех сервисов
REM Использование: scripts\rebuild.bat

echo ╔══════════════════════════════════════════════════════════════╗
echo ║          Alerting System - Полная пересборка                ║
echo ╚══════════════════════════════════════════════════════════════╝

REM Проверка наличия Docker
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker не найден.
    exit /b 1
)

REM Проверка наличия Docker Compose
where docker compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose не найден.
    exit /b 1
)

echo Остановка старых контейнеров...
docker compose down

echo Удаление старых volumes...
docker compose down -v

echo Сборка и запуск всех сервисов...
docker compose up --build -d

echo Ожидание готовности сервисов...
timeout /t 30 /nobreak >nul

echo.
echo Все сервисы запущены!
echo.
echo Статус сервисов:
docker compose ps
echo.
echo Логи: docker compose logs -f ^<service-name^>
echo Остановка: scripts\stop.bat
