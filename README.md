# Alerting System

Система потоковой обработки рыночных событий: фильтры создаются через API, трейды идут через Kafka, Flink проверяет условия и публикует алерты.

## Стек

- Java 25: `parser`, `filter-service`, `alert-service`
- Java 21: Flink job `data-processor`
- Gradle multi-module
- Apache Kafka 3.9.1, single broker, KRaft
- Apache Flink 2.2.0
- PostgreSQL 16 + Flyway
- Docker Compose
- Kora Framework

## Модули

| Модуль | Назначение |
|---|---|
| `parser` | Тестовый генератор трейдов в `trades-topic` |
| `filter-service` | API фильтров, PostgreSQL, outbox, публикация в `filter-topic` |
| `data-processor` | Flink job: читает трейды и фильтры, пишет алерты |
| `alert-service` | Читает `alert-topic`, отдает алерты наружу |
| `filter-outbox-common` | Общие DTO и JSON-маппинг outbox-событий |
| `client-desktop` | Клиентский модуль, не основной путь запуска сейчас |

## Kafka topics

| Topic | Назначение | Retention |
|---|---|---|
| `trades-topic` | Поток трейдов | 1 сек |
| `filter-topic` | События фильтров | 7 дней |
| `command-topic` | Резерв под команды | 7 дней |
| `alert-topic` | Быстрые уведомления | 5 сек |

Контракт `trades-topic`:

- key: `symbol` как UTF-8 string
- value: 16 bytes, little-endian
- `long price` offset `0`
- `long timestampNs` offset `8`

## Локальный запуск

Требования:

- JDK 25
- Docker Desktop
- Docker Compose

Собрать jar-файлы в `artifacts/`:

```powershell
.\gradlew.bat buildArtifacts
```

Запустить весь стенд:

```powershell
docker compose -f docker-compose.yml up -d
```

Проверить контейнеры:

```powershell
docker compose -f docker-compose.yml ps
```

Остановить:

```powershell
docker compose -f docker-compose.yml down
```

Удалить контейнеры и volumes:

```powershell
docker compose -f docker-compose.yml down -v
```

## Что запускает Compose

| Сервис | Порт | Комментарий |
|---|---:|---|
| `postgres` | `5432` | БД фильтров |
| `kafka` | `9092` | Один Kafka broker |
| `kafka-init` | - | Создает topics |
| `flink-jobmanager` | `8088` | Flink UI |
| `flink-taskmanager` | - | Flink worker |
| `flink-data-processor-submit` | - | Сабмитит `data-processor-job.jar` во Flink |
| `filter-service` | `8081`, `8086` | API фильтров |
| `parser` | `9080` | Тестовая генерация трейдов |
| `alert-service` | `8090`, `7818` | HTTP/WebSocket алертов |

Compose не собирает код. Он берет готовые jar-файлы из `artifacts/`.

## Flink job

Список job:

```powershell
docker exec -it flink-jobmanager flink list -m flink-jobmanager:8081
```

Отменить job:

```powershell
docker exec -it flink-jobmanager flink cancel -m flink-jobmanager:8081 <job-id>
```

Засабмитить заново:

```powershell
docker compose -f docker-compose.yml up flink-data-processor-submit
```

## Kafka

Список топиков:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092
```

Описание топика:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --describe --bootstrap-server kafka:9092 --topic trades-topic
```

Consumer groups:

```powershell
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --all-groups --describe
```

## Логи

Все логи:

```powershell
docker compose -f docker-compose.yml logs --tail 200
```

Конкретный сервис:

```powershell
docker compose -f docker-compose.yml logs -f --tail 200 parser
docker compose -f docker-compose.yml logs -f --tail 200 filter-service
docker compose -f docker-compose.yml logs -f --tail 200 alert-service
docker compose -f docker-compose.yml logs -f --tail 200 flink-data-processor-submit
docker compose -f docker-compose.yml logs -f --tail 200 flink-taskmanager
```

## Тестовый поток трейдов

После запуска стенда:

```powershell
Invoke-RestMethod http://localhost:9080/api/trades/generate
```

Если Kafka приняла сообщение, в логах parser будет `ProduceResponse` с `errorCode=0`.

## Сборка и тесты

Собрать все:

```powershell
.\gradlew.bat build
```

Собрать artifacts:

```powershell
.\gradlew.bat buildArtifacts
```

Тесты конкретного модуля:

```powershell
.\gradlew.bat :data-processor:test
.\gradlew.bat :filter-service:test
```

## Конфиги

Конфиги лежат внутри jar как resources. Для Docker-режима compose передает env-переменные:

- `KAFKA_BOOTSTRAP_SERVERS`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `KORA_CONFIG_APPLICATION`

## Production idea

Локально удобно запускать готовые jar через volume из `artifacts/`.

Для production обычно собирают Docker image на каждый сервис через `Dockerfile`, пушат image в registry и запускают уже immutable image, а не jar с ноутбука.
