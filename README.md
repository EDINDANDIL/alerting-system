# Alerting System

> **Платформа автоматического скрининга рыночных событий с низкой задержкой**

**Репозиторий:** [github.com/EDINDANDIL/alerting-system](https://github.com/EDINDANDIL/alerting-system)

---

## 📖 О проекте

**Alerting System** — это платформа для **автоматического отслеживания рыночных событий** в реальном времени. Система позволяет трейдерам задавать гибкие правила мониторинга (биржа, рынок, направление движения, процент изменения, объём) и мгновенно получать сигналы при срабатывании условий.

### Зачем это нужно

| Проблема | Решение |
|----------|---------|
| Ручной мониторинг бирж требует постоянного внимания | **Автоматический скрининг** 24/7 |
| Трейдер пропускает входы из-за большого потока данных | **Мгновенные алерты** при срабатывании условий |
| Реакция на события занимает секунды | **Задержка ~100 мс** end-to-end |
| Сложно отслеживать несколько инструментов одновременно | **Масштабируемая обработка** потока данных |

### Ценность для бизнеса

- **Снижение когнитивной нагрузки** — система отслеживает рынок вместо трейдера
- **Повышение скорости реакции** — сигнал приходит в момент появления ситуации
- **Гибкость настройки** — правила под любую торговую стратегию
- **Интеграция в торговый контур** — сигналы уходят прямо в терминал (Tiger Trade, MetaScalp, Vataga)

---

## Быстрый старт

### Вариант 1: Для пользователей (готовый JAR)

**Требования:** Java 25 (`java -version`)

```bash
cd <Путь до проекта>

.\run-client.bat
```

Или напрямую:
```bash
java -jar client-desktop/build/libs/client-desktop-1.0-SNAPSHOT-fat.jar
```

---

### Вариант 2: Для разработчиков (сборка с нуля)

**Требования:**
- JDK 25
- Docker + Docker Compose
- Gradle 9.x

#### Шаг 1: Сборка проекта

```bash
cd <Путь до проекта>

gradle build --no-daemon

gradle :client-desktop:fatJar --no-daemon
```

#### Шаг 2: Запуск инфраструктуры (Docker)

```bash
docker compose up --build
```

**Сервисы:**

| Сервис | Порт | Назначение |
|--------|------|------------|
| `filter-service` | 8081 | HTTP API для управления фильтрами |
| `alert-service` | 8090 / 7818 | WebSocket для доставки алертов |
| `data-processor` | 8082 | Потоковая обработка трейдов |
| `outbox-sender` | 9099 | Отправка событий в Kafka |
| `PostgreSQL` | 5432 | База данных фильтров |
| `Kafka` | 9092, 9094, 9096 | Шина событий |

#### Шаг 3: Запуск CLI клиента

```bash
.\run-client.bat

java -jar client-desktop/build/libs/client-desktop-1.0-SNAPSHOT-fat.jar

gradle :client-desktop:runInteractive --no-daemon
```

#### Шаг 4: Использование

```bash
help                   
filter create --user-id 1 --exchange binance --market futures --direction UP --percent 2
connect --user-id 1    
status                  
```

---

### Вариант 3: Только инфраструктура (без CLI)

```bash
docker compose up postgres kafka1 kafka2 kafka3 kafka-init -d

gradle :filter-service:run
gradle :outbox-sender:run
gradle :data-processor:run
gradle :alert-service:run
```

---

## 🏗 Архитектура

```
┌────────────────────────────────────────────────────────────────────────┐
│                         ALERTING SYSTEM                                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────┐     ┌─────────────────┐     ┌───────────────┐        │
│  │   Клиент     │ ──▶ │  filter-service │ ──▶ │  PostgreSQL   │        │
│  │  (HTTP/CLI)  │     │  (создание/     │     │  (фильтры,    │        │
│  │              │     │   удаление)     │     │   outbox)     │        │
│  └──────────────┘     └─────────────────┘     └───────┬───────┘        │
│                                                       │                │
│  ┌──────────────┐     ┌─────────────────┐     ┌───────▼───────┐        │
│  │   Клиент     │ ◀── │  alert-service  │ ◀── │  Kafka        │        │
│  │ (WebSocket)  │     │  (рассылка      │     │  alert-topic  │        │
│  │              │     │   алертов)      │     │               │        │
│  └──────────────┘     └─────────────────┘     └───────▲───────┘        │
│                                                       │                │
│  ┌──────────────┐     ┌─────────────────┐     ┌───────┴───────┐        │
│  │  C++ парсер  │ ──▶ │ data-processor  │ ◀── │  Kafka        │        │
│  │  (рынок)     │     │ (IMPULSE логика)│     │  filter-topic │        │
│  │              │     │                 │     │               │        │
│  └──────────────┘     └─────────────────┘     └───────────────┘        │
│                            │                                           │
│                            │ Kafka trades-topic                        │
│                            ▼                                           │
│                     ┌─────────────┐                                    │
│                     │ Tiger Trade │                                    │
│                     │             │                                    │
│                     └─────────────┘                                    │
└────────────────────────────────────────────────────────────────────────┘
```

### Поток данных (e2e)

1. **Создание фильтра**: Клиент → `POST /api/filters` → `filter-service` → PostgreSQL + outbox
2. **Публикация события**: `outbox-sender` → читает outbox → публикует в `filter-topic`
3. **Обработка фильтра**: `data-processor` → обновляет состояние фильтров в памяти
4. **Поток трейдов**: C++ парсер → `trades-topic` → `data-processor` → проверка по алгоритму **IMPULSE**
5. **Алерт**: При срабатывании → `alert-topic` → `alert-service` → WebSocket → CLI-клиент
6. **Интеграция**: CLI может автоматически отправлять сигнал в **Tiger Trade API**

---

## Структура проекта

```
alerting-system/
├── filter-service/           # HTTP API + PostgreSQL + Outbox
│   ├── src/main/java/ru/
│   ├── src/main/resources/
│   │   ├── application.conf  # Конфигурация
│   │   └── db/migration/     # Flyway миграции БД
│   └── Dockerfile
│
├── outbox-sender/            # Фоновый sender из outbox в Kafka
│   ├── src/main/java/ru/
│   ├── src/main/resources/application.conf
│   └── Dockerfile
│
├── filter-outbox-common/     # Общие DTO/мапперы/outbox-модель
│   └── src/main/java/ru/
│
├── data-processor/           # Потоковая обработка Kafka-событий
│   ├── src/main/java/ru/
│   ├── src/main/resources/application.conf
│   └── Dockerfile
│
├── alert-service/            # WebSocket сервер для доставки алертов
│   ├── src/main/java/ru/
│   ├── src/main/resources/application.conf
│   └── Dockerfile
│
├── client-desktop/           # CLI клиент с интеграцией Tiger Trade
│   ├── src/main/java/ru/alertcli/
│   ├── build.gradle
│   └── Dockerfile
│
├── docker-compose.yml        # Полный стек для локальной разработки
├── KafkaClusterDocker-compose.yaml  # Отдельный Kafka кластер
├── .env.example              # Шаблон переменных окружения
└── scripts/                  # Скрипты запуска
    ├── start.bat / start.sh
    ├── stop.bat / stop.sh
    └── rebuild.bat / rebuild.sh
```

---

## API

### Filter Service (`localhost:8081`)

#### POST `/api/filters` — Создать фильтр

```http
POST /api/filters
X-user-id: 101
Content-Type: application/json

{
  "action": "IMPULSE",
  "exchange": ["binance", "bybit"],
  "market": ["futures"],
  "blackList": ["btcusdc"],
  "timeWindow": 60,
  "direction": "UP",
  "percent": 2,
  "volume24h": 1000000
}
```

**Ответ:** `200 ok`

#### DELETE `/api/filters` — Удалить фильтр

```http
DELETE /api/filters
X-user-id: 101
Content-Type: application/json

{
  "action": "IMPULSE",
  "exchange": ["binance"],
  "market": ["futures"],
  "direction": "UP",
  "percent": 2
}
```

**Ответ:** `200 ok`

---

## 🗂 Топики Kafka

| Топик | Назначение | Партиции | Retention |
|-------|------------|----------|-----------|
| `trades-topic` | Входящие трейды (JSON `TradeEvent`) | 32 | 7 дней    |
| `filter-topic` | События фильтров (CREATE, SUBSCRIBE, UNSUBSCRIBE, DELETE) | 8 | 7 дней    |
| `alert-topic` | Алерты для доставки клиентам | 8 | 10 ms     |

**Ключ партиции для `trades-topic`:** `exchange|market|symbol`

---

## ⚙️ Конфигурация

### Переменные окружения

```bash
# Скопировать шаблон
cp .env.example .env
```

### Порты сервисов

| Сервис | Public порт | Private порт |
|--------|-------------|--------------|
| `filter-service` | 8081 | 8086 |
| `outbox-sender` | 9099 | 9089 |
| `data-processor` | 8082 | 8087 |
| `alert-service` | 8090 | — |
| `alert-service` (WebSocket) | 7818 | — |

### Базы данных

| Параметр | Значение |
|----------|----------|
| Хост | localhost:5432 |
| БД | filter_db |
| Пользователь | filter |
| Пароль | filter |

### Kafka

| Параметр | Значение |
|----------|----------|
| Bootstrap servers | localhost:9092, localhost:9094, localhost:9096 |
| Cluster ID | DqqvGAQURkej_lZD0qN_vA |

---

## Тесты

```bash
# Все тесты
gradle test

# Тесты конкретного модуля
gradle :filter-service:test
gradle :outbox-sender:test
gradle :data-processor:test
gradle :alert-service:test
```

В `outbox-sender` есть интеграционный тест с Testcontainers (`KafkaOutboxSenderIT`).

---

## 🛠 Технологический стек

| Категория | Технологии |
|-----------|------------|
| **Язык** | Java 25 |
| **Фреймворк** | Kora Framework |
| **Сборка** | Gradle 9.x |
| **БД** | PostgreSQL 16 + Flyway |
| **Шина событий** | Apache Kafka 3.9 |
| **Контейнеризация** | Docker Compose |
| **Тестирование** | JUnit 5 + Testcontainers |
| **CLI** | Picocli + JLine |
| **WebSocket** | Tyrus |

---

## Диагностика проблем

### Нет событий в `filter-topic`
- Проверить запуск `outbox-sender`: `docker compose logs outbox-sender`
- Проверить записи в `filter_outbox`: `SELECT * FROM filter_outbox ORDER BY created_at DESC LIMIT 10;`
- Проверить доступность Kafka: `docker compose ps kafka1`

### `POST /api/filters` падает
- Проверить заголовок `X-user-id`
- Проверить таблицы в PostgreSQL: `\dt` (в psql)
- Проверить логи Flyway: `docker compose logs filter-service | grep flyway`

### `data-processor` не реагирует на трейды
- Проверить сообщения в `trades-topic`
- Проверить `group.id` и `auto.offset.reset` в конфигурации
- Убедиться, что Kafka инициализировал топики

### Сервис не запускается
- Проверить статус: `docker compose ps`
- Проверить логи: `docker compose logs <service-name>`
- Проверить занятость портов: `netstat -ano | findstr :8081`

---

## Roadmap

- [ ] Полная публикация алертов в `alert-topic` из `data-processor`
- [ ] Интеграция с Tiger Trade API через CLI
- [ ] Авторизация и полноценный мультипользовательский режим
- [ ] Runbook и health-check сценарии для production
- [ ] Метрики и мониторинг (Prometheus + Grafana)
- [ ] Поддержка дополнительных типов фильтров (не только IMPULSE)
- [ ] REST API для управления подписками

---

## 📄 Лицензия

MIT

---

## Контакты

По вопросам и предложениям: [GitHub Issues](https://github.com/EDINDANDIL/alerting-system/issues)
