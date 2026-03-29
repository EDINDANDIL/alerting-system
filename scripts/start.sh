#!/bin/bash
# Скрипт для быстрого запуска всей инфраструктуры alerting-system
# Использование: ./scripts/start.sh

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Alerting System - Запуск инфраструктуры            ║"
echo "╚══════════════════════════════════════════════════════════════╝"

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo "Docker не найден. Пожалуйста, установите Docker."
    exit 1
fi

# Проверка наличия Docker Compose
if ! command -v docker compose &> /dev/null; then
    echo "Docker Compose не найден. Пожалуйста, установите Docker Compose."
    exit 1
fi

echo "Запуск PostgreSQL и Kafka кластера..."
docker compose up postgres kafka1 kafka2 kafka3 kafka-init -d

echo "Ожидание готовности сервисов..."
sleep 10

echo "Инфраструктура запущена!"
echo ""
echo "Сервисы:"
echo "   - PostgreSQL: localhost:5432 (filter_db / filter / filter)"
echo "   - Kafka: localhost:9092, localhost:9094, localhost:9096"
echo ""
echo "Топики Kafka:"
echo "   - trades-topic (32 партиции)"
echo "   - filter-topic (8 партиций)"
echo "   - alert-topic (8 партиций)"
echo "   - cxet.commands (4 партиции)"
echo ""
echo "Для запуска Java-сервисов выполните:"
echo "   gradle :filter-service:run"
echo "   gradle :outbox-sender:run"
echo "   cd data-processor && gradle run"
echo "   gradle :alert-service:run"
echo ""
echo "Для остановки выполните: ./scripts/stop.sh"
