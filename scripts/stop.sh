#!/bin/bash
# Скрипт для остановки всей инфраструктуры alerting-system
# Использование: ./scripts/stop.sh

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Alerting System - Остановка инфраструктуры         ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# Проверка наличия Docker Compose
if ! command -v docker compose &> /dev/null; then
    echo "Docker Compose не найден."
    exit 1
fi

echo "Остановка всех сервисов..."
docker compose down

echo ""
echo "Инфраструктура остановлена!"
echo ""
echo "Для очистки данных выполните: docker compose down -v"
