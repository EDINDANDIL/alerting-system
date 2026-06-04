# Alerting System

Система потоковой обработки рыночных событий.
Фильтры (impulse) создаются через REST API → уходят в Kafka → Flink применяет их к потоку трейдов → алерты публикуются в Kafka.

## Стек

- Java 25: `filter-service`, `parser`
- Java 21: `data-processor` (Flink job)
- Gradle multi-module
- Apache Kafka 3.9.1, single broker, KRaft
- Apache Flink 2.2.0
- PostgreSQL 16 + Flyway
- Docker Compose
- Kora Framework (Tinkoff)

## Модули

| Модуль | Назначение |
|---|---|
| `filter-outbox-common` | Общие DTO (`AlertCreatedEvent`, `FilterCreatedEvent`, `OutboxPayload`), сериализаторы, JDBC-мапперы для JSONB |
| `filter-service` | REST API фильтров + PostgreSQL + transactional outbox → `filter-topic`. Читает алерты из `alert-topic` |
| `data-processor` | Flink job: читает `trades-topic` (бинарный 16-байтный протокол) + `filter-topic` (broadcast), ImpulseStrategy, пишет `AlertCreatedEvent` в `alert-topic` |
| `parser` | Генератор тестовых трейдов в `trades-topic` |

## Kafka topics

| Topic | Partitions | Retention | Назначение |
|---|---|---|---|
| `trades-topic` | 32 | 1 с | Бинарный поток трейдов |
| `filter-topic` | 8 | 7 дн. | События жизненного цикла фильтров |
| `alert-topic` | 8 | 5 с | Алерты от Flink |
| `command-topic` | 8 | 7 дн. | Зарезервирован |

Контракт `trades-topic`:
- key: `symbol` (UTF-8)
- value: 16 bytes little-endian
  - `long price` offset 0
  - `long timestampNs` offset 8

## Архитектура

```
parser ──(trades-topic)──► Flink data-processor ──(alert-topic)──► Kafka
                                   ▲
filter-service ──(filter-topic)────┘
      ▲
      │ REST API
      │ POST /api/filters/{type}       — создать/подписаться
      │ DELETE /api/filters/{type}/{id} — отписаться
      │ GET  /api/filters              — список фильтров
      │
PostgreSQL (impulse_filters + user_impulse_filters + filter_outbox)
```

## Data flow

1. Клиент создаёт фильтр через `POST /api/filters/IMPULSE`.
2. `filter-service` пишет в `impulse_filters` + `filter_outbox`.
3. Планировщик читает outbox и публикует `FilterCreatedEvent` в `filter-topic`.
4. Flink job получает фильтр (broadcast state) и начинает применять его к входящим трейдам.
5. При срабатывании `ImpulseStrategy` Flink публикует `AlertCreatedEvent` в `alert-topic`.
6. `filter-service` потребляет `alert-topic`.

## Локальный запуск

Требования: JDK 25, Docker Desktop.

```powershell
.\gradlew.bat buildArtifacts
docker compose up -d
```

Проверить контейнеры:

```powershell
docker compose ps
```

Остановить:

```powershell
docker compose down
```

## Сервисы compose

| Сервис | Порт | Назначение |
|---|---|---|
| `postgres` | 5432 | БД фильтров |
| `kafka` | 9092 | Kafka broker (KRaft) |
| `kafka-init` | — | Создание топиков |
| `flink-jobmanager` | 8088 | Flink UI |
| `flink-taskmanager` | — | Flink worker |
| `flink-data-processor-submit` | — | Сабмит job во Flink |
| `filter-service` | 8081, 8086 | REST API |
| `cxet-kafka-service` | — | C++ CXET-коннектор |

Compose не собирает код — использует jar из `artifacts/`.

## Flink job

Список job:

```powershell
docker exec flink-jobmanager flink list -m flink-jobmanager:8081
```

Отменить job:

```powershell
docker exec flink-jobmanager flink cancel -m flink-jobmanager:8081 <job-id>
```

Перезапустить submit:

```powershell
docker compose up flink-data-processor-submit
```

## Kafka

Список топиков:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092
```

Описание топика:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --describe --bootstrap-server kafka:9092 --topic trades-topic
```

Consumer groups:

```powershell
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --all-groups --describe
```

## Генерация тестовых трейдов

Parser закомментирован в compose. Для ручного запуска:

```powershell
java -jar artifacts\parser.jar
Invoke-RestMethod http://localhost:9080/api/trades/generate
```

## Сборка и тесты

```powershell
.\gradlew.bat buildArtifacts
.\gradlew.bat :data-processor:test
.\gradlew.bat :filter-service:test
```

## Переменные окружения

| Переменная | Назначение |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | Адреса Kafka |
| `DB_URL` | JDBC URL PostgreSQL |
| `DB_USERNAME` | Пользователь БД |
| `DB_PASSWORD` | Пароль БД |
| `KORA_CONFIG_APPLICATION` | Конфиг (docker/local) |

## Логи

```powershell
docker compose logs --tail 200
docker compose logs -f --tail 200 filter-service
docker compose logs -f --tail 200 flink-data-processor-submit
```
