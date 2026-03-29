@echo off
REM Скрипт для быстрого запуска всей инфраструктуры alerting-system
REM Использование: scripts\start.bat

echo ╔══════════════════════════════════════════════════════════════╗
echo ║          Alerting System - Запуск инфраструктуры            ║
echo ╚══════════════════════════════════════════════════════════════╝

REM Проверка наличия Docker
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker не найден. Пожалуйста, установите Docker.
    exit /b 1
)

REM Проверка наличия Docker Compose
where docker compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose не найден. Пожалуйста, установите Docker Compose.
    exit /b 1
)

echo Запуск PostgreSQL и Kafka кластера...
docker compose up postgres kafka1 kafka2 kafka3 kafka-init -d

echo Ожидание готовности сервисов...
timeout /t 10 /nobreak >nul

echo.
echo Инфраструктура запущена!
echo.
echo Сервисы:
echo    - PostgreSQL: localhost:5432 ^(^filter_db^ / filter / filter^)^
echo    - Kafka: localhost:9092, localhost:9094, localhost:9096
echo.
echo Топики Kafka:
echo    - trades-topic ^(^32 партиции^)^
echo    - filter-topic ^(^8 партиций^)^
echo    - alert-topic ^(^8 партиций^)^
echo    - cxet.commands ^(^4 партиции^)^
echo.
echo Для запуска Java-сервисов выполните:
echo    gradle :filter-service:run
echo    gradle :outbox-sender:run
echo    cd data-processor ^&^& gradle run
echo    gradle :alert-service:run
echo.
echo Для остановки выполните: scripts\stop.bat
