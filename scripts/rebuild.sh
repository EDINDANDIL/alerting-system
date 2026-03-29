#!/bin/bash
# Скрипт для полной пересборки и запуска всех сервисов
# Использование: ./scripts/rebuild.sh

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Alerting System - Полная пересборка                ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo "Docker не найден."
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "Docker Compose не найден."
    exit 1
fi

echo "Остановка старых контейнеров..."
docker compose down

echo "Удаление старых volumes..."
docker compose down -v

echo "Сборка и запуск всех сервисов..."
docker compose up --build -d

echo "Ожидание готовности сервисов..."
sleep 30

echo ""
echo "Все сервисы запущены!"
echo ""
echo "Статус сервисов:"
docker compose ps
echo ""
echo "Логи: docker compose logs -f <service-name>"
echo "Остановка: ./scripts/stop.sh"
