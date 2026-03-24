# alerting-system

MVP-система алертинга на Java (Kora) с outbox-паттерном и Kafka:

- `filter-service` принимает HTTP-запросы на подписку/отписку фильтров.
- События записываются в таблицу outbox в PostgreSQL.
- `outbox-sender` периодически читает outbox и публикует события в Kafka (`filter-topic`).
- `data-processor` принимает `filter-topic` и `trades-topic`, применяет фильтры и формирует алерты.

## Технологии

- Java 25
- Gradle
- [Tinkoff Kora]
- PostgreSQL
- Kafka

## Структура репозитория

- `filter-service` - HTTP API + запись фильтров и outbox в PostgreSQL
- `outbox-sender` - фоновый sender из outbox в Kafka
- `filter-outbox-common` - общие DTO/мапперы/outbox-модель
- `data-processor` - отдельный Gradle-проект для обработки Kafka-событий
- `KafkaClusterDocker-compose.yaml` - локальный Kafka-кластер (3 брокера)

## Поток данных (e2e)

1. Клиент вызывает `POST /api/filters` (или `DELETE /api/filters`) в `filter-service`.
2. `filter-service` обновляет таблицы фильтров и пишет событие в `filter_outbox`.
3. `outbox-sender` (каждые 50ms) читает `filter_outbox` в транзакции и публикует в Kafka `filter-topic`.
4. После успешной отправки запись outbox удаляется.
5. `data-processor` читает:
   - `filter-topic` -> обновляет внутреннее состояние фильтров;
   - `trades-topic` -> прогоняет trade через процессор.
6. Алерты сейчас отправляются в лог (`AlertPublisher`), без отдельного Kafka producer.

## Порты сервисов

- `filter-service`: public `8081`, private `8086`
- `data-processor`: public `8082`, private `8087`
- `outbox-sender`: public `8084`, private `8089`

## Топики Kafka

- `trades-topic` - входящие трейды (JSON `TradeEvent`)
- `filter-topic` - события фильтров из outbox

## Требования

- JDK 25
- Docker + Docker Compose
- PostgreSQL (локально на `localhost:5432`, БД `filter_db`, пользователь/пароль `filter/filter`)
- Gradle (для запуска модулей из корня)

## Быстрый старт

### 1) Поднять Kafka

```bash
docker compose -f KafkaClusterDocker-compose.yaml up -d
```

### 2) Подготовить PostgreSQL

Сервисам нужны таблицы:

- `impulse_filters`
- `user_impulse_filters`
- `filter_outbox`

В репозитории нет полноценных SQL-миграций для всех таблиц, поэтому схему нужно создать вручную или через ваш migration tool.

### 3) Собрать проект

Из корня:

```bash
gradle build
```

Для `data-processor` можно использовать wrapper:

```bash
cd data-processor
./gradlew build
```

### 4) Запустить сервисы

В отдельных терминалах:

```bash
gradle :filter-service:run
```

```bash
gradle :outbox-sender:run
```

```bash
cd data-processor
./gradlew run
```

## API (`filter-service`)

### POST `/api/filters`

Создает/привязывает фильтр к пользователю. Требуется заголовок `X-user-id`.

Пример:

```http
POST /api/filters
X-user-id: 101
Content-Type: application/json

{
  "action": "IMPULSE",
  "exchange": ["binance"],
  "market": ["futures"],
  "blackList": ["btcusdc"],
  "timeWindow": 60,
  "direction": "UP",
  "percent": 2,
  "volume24h": 1000000
}
```

Ответ: `200 ok`

### DELETE `/api/filters`

Отписывает пользователя от фильтра. Тело и заголовки аналогичны `POST`.

Ответ: `200 ok`

## Конфигурация

Основные конфиги:

- `filter-service/src/main/resources/application.conf`
- `outbox-sender/src/main/resources/application.conf`
- `data-processor/src/main/resources/application.conf`

По умолчанию используются локальные bootstrap-серверы Kafka:

- `localhost:9092`
- `localhost:9094`
- `localhost:9096` (для `data-processor`)

## Тесты

Из корня:

```bash
gradle test
```

Для `data-processor`:

```bash
cd data-processor
./gradlew test
```

В `outbox-sender` есть интеграционный тест с Testcontainers (`KafkaOutboxSenderIT`).

## Текущие ограничения MVP

- Полный Docker Compose для всех Java-сервисов и PostgreSQL пока не добавлен.
- Алерты в `data-processor` пока логируются, а не публикуются в отдельный topic.
- SQL-миграции для всех таблиц в репозитории пока неполные.

## Диагностика проблем

- Нет событий в `filter-topic`:
  - проверить запуск `outbox-sender`;
  - проверить наличие записей в `filter_outbox`;
  - проверить доступность Kafka bootstrap-серверов.
- `POST /api/filters` падает:
  - проверить заголовок `X-user-id`;
  - проверить наличие и корректность таблиц в PostgreSQL.
- `data-processor` не реагирует на трейды:
  - проверить, что сообщения действительно приходят в `trades-topic`;
  - проверить `group.id` и `auto.offset.reset` в `data-processor/application.conf`.

## Планы на будущее

- Добавить Flyway/Liquibase миграции для всех таблиц.
- Добавить единый `docker-compose` для PostgreSQL + 3 сервисов + Kafka.
- Публиковать алерты в отдельный Kafka topic (например, `alerts-topic`).
- Добавить runbook и health-check сценарии для прод-подобного запуска.
