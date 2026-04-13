# Data Processor

Сервис обработки торговых событий (trades) и детекции ценовых импульсов в реальном времени.

---

## Оглавление

1. [Обзор](#1-обзор)
2. [Технологический стек](#2-технологический-стек)
3. [Структура модуля](#3-структура-модуля)
4. [Конфигурация сборки](#4-конфигурация-сборки)
5. [Docker](#5-docker)
6. [Конфигурация приложения](#6-конфигурация-приложения)
7. [Точка входа](#7-точка-входа)
8. [Kafka — потребители и продюсер](#8-kafka--потребители-и-продюсер)
9. [Модели данных (DTO)](#9-модели-данных-dto)
10. [Enums и утилиты](#10-enums-и-утилиты)
11. [Кэш — управление состоянием фильтров](#11-кэш--управление-состоянием-фильтров)
12. [Скользящее окно (SlidingWindow)](#12-скользящее-окно-slidingwindow)
13. [Domain — движок фильтрации](#13-domain--движок-фильтрации)
14. [Сервисы — оркестрация](#14-сервисы--оркестрация)
15. [JSON-мапперы](#15-json-мапперы)
16. [Полный поток данных](#16-полный-поток-данных)
17. [Конкурентная модель](#17-конкурентная-модель)
18. [Тестирование](#18-тестирование)
19. [Запуск](#19-запуск)
20. [Известные ограничения и TODO](#20-известные-ограничения-и-todo)
21. [Расширяемость](#21-расширяемость)

---

## 1. Обзор

**data-processor** — сервис обработки торговых событий (trades) в системе алертинга.

### Задачи

- Потреблять торговые сделки из Kafka-топика `trades-topic`
- Потреблять события управления фильтрами из Kafka-топика `filter-topic` (CREATE/DELETE/SUBSCRIBE/UNSUBSCRIBE)
- В реальном времени анализировать ценовые импульсы по каждому символу в скользящих окнах
- Публиковать алерты в Kafka-топик `alert-topic` при срабатывании фильтров

### Архитектурные решения

| Решение | Описание |
|---|---|
| **Event-driven** | Нет REST-контроллеров, БД или scheduled-задач. Всё работает через Kafka |
| **In-memory** | Всё состояние в памяти (`ConcurrentHashMap`, `HashMap`). Нет зависимости от БД |
| **Группировка по timeWindow** | Фильтры с одинаковым размером окна используют **одно** SlidingWindow. Один проход по данным — проверка всех фильтров |
| **Monotonic deque** | Amortized O(1) нахождение min/max цены в скользящем окне |
| **Lazy eviction (TTL 60s)** | Окна, в которые не было записей > 60 секунд, удаляются при следующем обращении (реализовано в `getOrComputeWindow` и `getWindows`) |
| **Poison pill protection** | Ошибки обработки filter-событий логируются и пропускаются — consumer не застревает |

---

## 2. Технологический стек

| Технология | Версия | Назначение |
|---|---|---|
| Java | 25 | Язык |
| Kora Framework | 1.2.12 | DI-фреймворк (от Тинькофф) |
| Gradle | 9.2.0 | Система сборки |
| Apache Kafka | — | Обмен сообщениями |
| HOCON | — | Формат конфигурации |
| Undertow | — | HTTP-сервер (для health/metrics) |
| Logback | 1.4.8 | Логирование |
| Jackson | (через Kora) | JSON-сериализация |
| JUnit 5 | 5.10.0 | Тестирование |
| Mockito | 5.18.0 | Моки в тестах |

---

## 3. Структура модуля

```
data-processor/
├── build.gradle
├── settings.gradle
├── Dockerfile
├── gradle/wrapper/gradle-wrapper.properties
├── src/main/
│   ├── resources/
│   │   ├── application.conf
│   │   └── application-docker.conf
│   └── java/ru/
│       ├── Application.java                          # Точка входа (@KoraApp)
│       │
│       ├── common/
│       │   ├── dto/
│       │   │   ├── OutboxCreatedEvent.java           # DTO outbox-события
│       │   │   └── OutboxPayload.java                # Sealed interface с payload
│       │   ├── mappers/direction/
│       │   │   ├── DirectionJsonReader.java           # JsonReader<Direction>
│       │   │   └── DirectionJsonWriter.java           # JsonWriter<Direction>
│       │   └── util/
│       │       ├── Direction.java                    # enum: UP/DOWN/BOTH
│       │       ├── OutboxOperation.java              # enum: CREATE/DELETE/SUBSCRIBE/UNSUBSCRIBE
│       │       └── OutboxTopicResolver.java          # Маппинг action → Kafka topic
│       │
│       ├── core/
│       │   ├── cache/
│       │   │   ├── WindowStore.java                  # Хранилище скользящих окон (с TTL eviction)
│       │   │   ├── TradesFilterSection.java           # Интерфейс секции фильтра
│       │   │   ├── TradesStateStore.java             # Store состояний (All<TradesFilterSection>)
│       │   │   └── ImpulseTradesSection.java          # Реализация impulse-секции
│       │   ├── engine/
│       │   │   └── FilterEngine.java                 # Движок проверки фильтров
│       │   └── util/
│       │       └── SlidingWindow.java                # Скользящее окно (monotonic deque)
│       │
│       ├── kafka/
│       │   ├── listeners/
│       │   │   ├── TradesListener.java               # Consumer trades-topic
│       │   │   └── EventsListener.java               # Consumer filter-topic
│       │   └── publishers/
│       │       └── AlertPublisher.java               # Producer alert-topic
│       │
│       ├── models/
│       │   ├── domain/
│       │   │   ├── TradePoint.java                   # record: timestampNs + priceRaw
│       │   │   └── FilterKey.java                    # record: action + filterId
│       │   ├── states/
│       │   │   ├── ImpulseFilterState.java            # Мутабельное состояние импульсного фильтра
│       │   │   └── ImpulseFilterView.java             # Иммутабельный view фильтра
│       │   └── dto/
│       │       ├── TradeEvent.java                   # DTO торговой сделки
│       │       └── AlertEvent.java                   # DTO алерта
│       │
│       └── services/
│           ├── TradeProcessor.java                   # Главный оркестратор
│           └── handlers/
│               ├── FilterEventHandler.java            # Интерфейс обработчика filter-событий
│               ├── FilterEventHandlerRegistry.java    # Реестр обработчиков (All<>)
│               └── ImpulseFilterEventHandler.java     # Обработчик IMPULSE-событий
│
└── src/test/java/ru/
    ├── common/util/
    │   ├── DirectionTest.java
    │   ├── OutboxOperationTest.java
    │   └── OutboxTopicResolverTest.java
    ├── core/
    │   ├── FilterKeyTest.java
    │   └── trades/
    │       ├── TradePointTest.java
    │       └── impulse/
    │           ├── ImpulseFilterStateTest.java
    │           ├── ImpulseFilterViewTest.java
    │           └── ImpulseTradesSectionTest.java
    ├── domain/
    │   └── FilterEngineTest.java
    ├── services/
    │   └── TradeProcessorIntegrationTest.java
    └── util/
        └── SlidingWindowTest.java
```

**Всего:** 23 main-файла, 13 test-файлов.

---

## 4. Конфигурация сборки

### `build.gradle`

```groovy
plugins {
    id "java"
    id "application"
}

group = 'ru'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}

application {
    mainClass = 'ru.Application'
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// Fat JAR для деплоя
tasks.register('fatJar', Jar) {
    dependsOn configurations.runtimeClasspath
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    with jar
    manifest {
        attributes 'Main-Class': 'ru.Application'
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = 'fat'
}

build.dependsOn fatJar

// Kora BOM
configurations {
    koraBom
    annotationProcessor.extendsFrom(koraBom)
    compileOnly.extendsFrom(koraBom)
    implementation.extendsFrom(koraBom)
    api.extendsFrom(koraBom)
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.junit.jupiter:junit-jupiter-params'
    testImplementation "ru.tinkoff.kora:test-junit5"
    testImplementation "org.mockito:mockito-core:5.18.0"

    koraBom platform("ru.tinkoff.kora:kora-parent:1.2.12")
    annotationProcessor "ru.tinkoff.kora:annotation-processors"

    implementation "ru.tinkoff.kora:http-server-undertow"
    implementation "ru.tinkoff.kora:json-module"
    implementation "ru.tinkoff.kora:config-hocon"
    implementation "ch.qos.logback:logback-classic:1.4.8"
    implementation "ru.tinkoff.kora:kafka"
    annotationProcessor "org.mapstruct:mapstruct-processor:1.5.5.Final"
    implementation "org.mapstruct:mapstruct:1.5.5.Final"
    implementation "ru.tinkoff.kora:logging-logback"
    testImplementation "org.mockito:mockito-core:5.18.0"
}

test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams(true)
        events("passed", "skipped", "failed")
        exceptionFormat("full")
    }
}
```

### `settings.gradle`

```groovy
rootProject.name = 'data-processor'
```

### `gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.0-bin.zip
```

---

## 5. Docker

### `Dockerfile`

Многоэтапная сборка:

```dockerfile
# Этап 1: сборка
FROM gradle:9.1.0-jdk25 AS builder
WORKDIR /app
COPY . .
RUN gradle :data-processor:fatJar --no-daemon

# Этап 2: runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
COPY --from=builder --chown=appuser:appgroup /app/data-processor/build/libs/*-fat.jar app.jar
USER appuser
EXPOSE 8082 8087
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/health || exit 1
ENV KORA_CONFIG_APPLICATION=application-docker.conf
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Порты:**
- `8082` — public API (health check `/health`)
- `8087` — private API

> **Примечание:** JVM запускается без явных флагов `-Xmx`/`-Xms` и без указания GC. Для production рекомендуется добавить параметры heap и GC (например, ZGC).

---

## 6. Конфигурация приложения

### `application.conf` / `application-docker.conf`

Оба файла идентичны. `application.conf` содержит дефолтные значения, `application-docker.conf` используется в Docker-окружении с переменной `${KAFKA_BOOTSTRAP_SERVERS}`.

```hocon
httpServer {
  publicApiHttpPort = 8082
  privateApiHttpPort = 8087
}

kafka {

    TradesConsumer {
        topics = ["trades-topic"]
        offset = "latest"
        pollTimeout = "5s"
        threads = 1
        shutdownWait = "30s"

        driverProperties {
            "bootstrap.servers" = ${KAFKA_BOOTSTRAP_SERVERS}
            "group.id" = "data-processor-trades-group"
            "key.deserializer" = "org.apache.kafka.common.serialization.StringDeserializer"
            "enable.auto.commit" = false
            "auto.offset.reset" = "latest"
            "max.poll.records" = 500
            "fetch.min.bytes" = 1
            "fetch.max.wait.ms" = 10
        }
    }

    eventConsume {
        topics = ["filter-topic"]
        offset = "earliest"
        pollTimeout = "5s"
        threads = 1
        shutdownWait = "30s"

        driverProperties {
            "bootstrap.servers" = ${KAFKA_BOOTSTRAP_SERVERS}
            "group.id" = "data-processor-filter-events-group"
            "key.deserializer" = "org.apache.kafka.common.serialization.StringDeserializer"
            "enable.auto.commit" = false
            "auto.offset.reset" = "earliest"
            "max.poll.records" = 200
            "fetch.max.wait.ms" = 10
        }
    }

    alertProducer {
        topics = ["alert-topic"]
        driverProperties {
            "bootstrap.servers" = ${KAFKA_BOOTSTRAP_SERVERS}
            "key.serializer" = "org.apache.kafka.common.serialization.StringSerializer"
            "value.serializer" = "org.apache.kafka.common.serialization.StringSerializer"
            "enable.idempotence" = true
            "acks" = "all"
            "retry.backoff.ms" = 100
            "linger.ms" = 5
        }
    }
}

logging {
    levels {
        "ROOT" = "INFO"
        "ru" = "DEBUG"
        "org.apache.kafka" = "WARN"
    }
}

kafka.TradesConsumer.telemetry.logging.enabled = false
kafka.eventConsume.telemetry.logging.enabled = false
```

**Ключевые параметры:**

| Параметр | Значение | Описание |
|---|---|---|
| `TradesConsumer.offset` | `latest` | Читать только новые трейды |
| `TradesConsumer.max.poll.records` | 500 | Батч до 500 сообщений |
| `TradesConsumer.threads` | 1 | Один поток обработки |
| `eventConsume.offset` | `earliest` | Прочитать все события фильтров |
| `alertProducer.enable.idempotence` | `true` | Idempotent producer (ровно-однократная доставка) |
| `alertProducer.acks` | `all` | Все реплики подтверждают |
| `alertProducer.linger.ms` | 5 | Батчинг алертов до 5 мс |

---

## 7. Точка входа

### `Application.java`

```java
@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        JsonModule,
        KafkaModule,
        LogbackModule {

    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }
}
```

`ApplicationGraph` генерируется автоматически annotation processor'ом Kora. Подключённые модули:

| Модуль | Что даёт |
|---|---|
| **HoconConfigModule** | Чтение HOCON-конфигурации |
| **UndertowHttpServerModule** | HTTP-сервер Undertow |
| **JsonModule** | Сериализация/десериализация JSON (Jackson) |
| **KafkaModule** | Интеграция с Kafka (`@KafkaListener`, `@KafkaPublisher`) |
| **LogbackModule** | Логирование через Logback |

---

## 8. Kafka — потребители и продюсер

### TradesListener — потребитель трейдов

```java
@Component
public class TradesListener {

    private final TradeProcessor tradesProcessor;
    private static final Logger log = LoggerFactory.getLogger(TradesListener.class);

    @KafkaListener("kafka.TradesConsumer")
    void handle(String key, @Json TradeEvent event, @Log.off Exception exception) {
        if (exception != null) return;
        log.debug("Приняли событие {}", key);
        tradesProcessor.process(key, event);
    }
}
```

Подписан на `trades-topic`. Делегирует обработку в `TradeProcessor`. Логирование на уровне `DEBUG` (не `INFO`) во избежание I/O bottleneck при высоком throughput.

### EventsListener — потребитель событий управления фильтрами

```java
@Component
public final class EventsListener {

    private static final Logger log = LoggerFactory.getLogger(EventsListener.class);
    private final FilterEventHandlerRegistry registry;

    @KafkaListener("kafka.eventConsume")
    void handle(String key, @Json OutboxCreatedEvent event, @Log.off Exception exception) {
        if (exception != null) {
            log.warn("Ошибка {}", exception.getMessage());
            return;
        }
        log.debug("FILTER_EVENT key={} action={} op={} filterId={} ...", ...);

        try {
            registry.get(event.action()).apply(event);
            log.debug("FILTER_EVENT applied: action={} op={} filterId={}", ...);
        } catch (RuntimeException e) {
            // Poison pill protection: логируем и пропускаем — consumer не застревает
            log.error("FILTER_EVENT apply failed: action={} op={} filterId={} -- skipping to avoid poison pill",
                    event.action(), event.operation(), event.filterId(), e);
        }
    }
}
```

Подписан на `filter-topic`. Находит обработчик по `action` и применяет событие. **Ошибки не пробрасываются** — это предотвращает «poison pill» ситуацию, когда malformed событие бесконечно re-deliver'ится.

### AlertPublisher — продюсер алертов

```java
@KafkaPublisher("kafka.alertProducer")
public interface AlertPublisher {
    CompletableFuture<RecordMetadata> send(ProducerRecord<String, @Json AlertEvent> record);
}
```

Kora автоматически генерирует реализацию интерфейса на основе аннотации `@KafkaPublisher`. Отправляет в `alert-topic` асинхронно (`CompletableFuture`).

---

## 9. Модели данных (DTO)

### TradeEvent

Торговая сделка, приходящая из `trades-topic`.

```java
@Json
public record TradeEvent(
    String exchange,       // "binance"
    String market,         // "futures"
    String symbol,         // "btcusdt"
    long id,               // trade id
    long timestampNs,      // наносекунды
    long firstTradeId,
    long lastTradeId,
    long price,            // raw-цена (long)
    long amount,           // raw-объём
    long quoteQty,         // raw-сумма сделки
    int side,              // 0 = buy, 1 = sell
    int isBuyerMaker       // 0 = false, 1 = true
) {}
```

### AlertEvent

Алерт, публикуемый в `alert-topic`.

```java
@Json
public record AlertEvent(
    Set<Integer> subscribers,  // ID пользователей-подписчиков
    List<String> exchange,     // биржи фильтра
    List<String> market,       // рынки фильтра
    String symbol,             // символ
    long timestampNs           // время алерта
) {}
```

### OutboxCreatedEvent

Событие управления фильтром из `filter-topic`.

```java
@Json
public record OutboxCreatedEvent(
    String action,               // "IMPULSE"
    OutboxOperation operation,   // CREATE/DELETE/SUBSCRIBE/UNSUBSCRIBE
    long filterId,               // ID фильтра
    int userId,                  // ID пользователя
    OffsetDateTime createdAt,    // время создания
    @Nullable OutboxPayload payload  // данные фильтра
) {}
```

### OutboxPayload — sealed interface

```java
@Json
@JsonDiscriminatorField("action")
public sealed interface OutboxPayload {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilter(
        List<String> exchange,   // биржи
        List<String> market,     // рынки
        List<String> blackList,  // чёрный список символов
        int timeWindow,          // окно в секундах
        Direction direction,     // UP/DOWN/BOTH
        int percent,             // порог изменения (%)
        int volume24h            // мин. объём за 24ч (не используется)
    ) implements OutboxPayload {}
}
```

Использует `@JsonDiscriminatorField("action")` для полиморфной десериализации — Kora автоматически выбирает нужный подтип по полю `action`.

> **Примечание:** Поле `volume24h` определено, но **не используется** в логике обработки. Зарезервировано для будущего функционала.

---

## 10. Enums и утилиты

### Direction

Направление движения цены.

```java
public enum Direction {
    UP((short) 0),     // Рост
    DOWN((short) 1),   // Падение
    BOTH((short) 2);   // Любое направление

    public final short code;

    public static Direction fromCode(short code) {
        return switch (code) {
            case 0 -> UP;
            case 1 -> DOWN;
            case 2 -> BOTH;
            default -> throw new IllegalArgumentException("Unknown direction code: " + code);
        };
    }
}
```

Сериализуется в JSON как числовой код (через `DirectionJsonReader`/`DirectionJsonWriter`).

### OutboxOperation

Операция над фильтром.

```java
@Json
public enum OutboxOperation {
    CREATE(0),         // Создать фильтр
    DELETE(1),         // Удалить фильтр
    SUBSCRIBE(2),      // Подписаться на алерты
    UNSUBSCRIBE(3);    // Отписаться

    private final int code;

    public int getCode() { return code; }

    public static OutboxOperation fromCode(int code) {
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> DELETE;
            case 2 -> SUBSCRIBE;
            case 3 -> UNSUBSCRIBE;
            default -> throw new IllegalArgumentException("Unknown status code: " + code);
        };
    }
}
```

### OutboxTopicResolver

Маппинг action → Kafka topic.

```java
public final class OutboxTopicResolver {

    private OutboxTopicResolver() {}

    public static String topicForAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be null or blank");
        }
        return switch (action) {
            case "IMPULSE" -> "Trades";
            default -> throw new IllegalArgumentException("Unknown action for topic: " + action);
        };
    }
}
```

---

## 11. Кэш — управление состоянием фильтров

### FilterKey

Уникальный ключ фильтра: комбинация `action` + `filterId`.

```java
public record FilterKey(String action, long filterId) {}
```

### TradesFilterSection

Интерфейс секции фильтра трейдов.

```java
public interface TradesFilterSection {
    String action();                     // "IMPULSE"
    void apply(OutboxCreatedEvent event);
}
```

### TradesStateStore

Store состояний. Собирает все `TradesFilterSection` через Kora `All<>` и делегирует обработку нужной секции.

```java
@Component
public final class TradesStateStore {
    private final Map<String, TradesFilterSection> sections;

    public TradesStateStore(All<TradesFilterSection> sections) {
        this.sections = sections.stream()
            .collect(Collectors.toMap(TradesFilterSection::action, s -> s));
    }

    public void apply(OutboxCreatedEvent event) {
        TradesFilterSection section = sections.get(event.action());
        if (section == null)
            throw new IllegalArgumentException("Unsupported Trades action: " + event.action());
        section.apply(event);
    }
}
```

### TradePoint

Точка данных: время + цена.

```java
public record TradePoint(long timestampNs, long priceRaw) {}
```

---

## Impulse — реализация импульсного фильтра

### ImpulseFilterState

Мутабельное состояние одного импульсного фильтра.

```java
public final class ImpulseFilterState {

    private final OutboxPayload.ImpulseFilter payload;
    private final Set<Integer> subscribers = ConcurrentHashMap.newKeySet();

    public ImpulseFilterState(OutboxPayload.ImpulseFilter payload) {
        this.payload = payload;
    }

    public OutboxPayload.ImpulseFilter payload() { return payload; }
    public Set<Integer> subscribers() { return subscribers; }
}
```

> Подписчики хранятся в `ConcurrentHashMap.newKeySet()` — потокобезопасный Set.

### ImpulseFilterView

Иммутабельный снимок фильтра для чтения (используется в hot path обработки трейдов).

```java
public record ImpulseFilterView(
    long filterId,
    OutboxPayload.ImpulseFilter payload,
    Set<Integer> subscribers
) {}
```

### ImpulseTradesSection

**Главный класс управления состоянием импульсных фильтров.** Хранит всё in-memory, обрабатывает CRUD-операции.

```java
@Component
public final class ImpulseTradesSection implements TradesFilterSection {

    // Все фильтры: (action, filterId) → состояние
    private final Map<FilterKey, ImpulseFilterState> impulseFilters = new HashMap<>();

    // Снимок для быстрого чтения (volatile)
    private volatile List<ImpulseFilterView> activeImpulseSnapshot = List.of();

    // Группировка по timeWindow (ns): для быстрого lookup при обработке трейдов
    private final Map<Long, Set<ImpulseFilterView>> filtersByWindow = new ConcurrentHashMap<>();

    // Чёрный список символов: symbol → Set<filterId>
    private final Map<String, Set<Long>> blacklist = new ConcurrentHashMap<>();

    @Override public String action() { return "IMPULSE"; }

    @Override
    public void apply(OutboxCreatedEvent event) {
        switch (event.operation()) {
            case CREATE -> createFilter(event);
            case DELETE -> deleteFilter(event);
            case SUBSCRIBE -> subscribe(event);
            case UNSUBSCRIBE -> unsubscribe(event);
        }
        rebuildSnapshot();
    }
}
```

**Операции:**

| Операция | Синхронизация | Описание |
|---|---|---|
| **CREATE** | `synchronized` | Создаёт фильтр, добавляет в `impulseFilters`, `filtersByWindow`, `blacklist` |
| **DELETE** | `synchronized` | Удаляет фильтр из всех структур |
| **SUBSCRIBE** | без lock | Добавляет `userId` в `subscribers` (ConcurrentHashMap.newKeySet), обновляет **один** view — **O(1)** |
| **UNSUBSCRIBE** | без lock | Удаляет `userId` из `subscribers`, обновляет **один** view — **O(1)** |

**Почему subscribe/unsubscribe без `synchronized`?** Они не меняют структуру `impulseFilters` — только модифицируют `subscribers` (потокобезопасный `ConcurrentHashMap.newKeySet()`) и обновляют один view в `filtersByWindow`.

**Публичные методы:**

```java
public List<ImpulseFilterView> activeImpulseFilters()     // снимок всех фильтров
public Map<Long, Set<ImpulseFilterView>> filtersByWindow() // группировка по timeWindow
public boolean isBlacklisted(String symbol, long filterId) // проверка blacklist
public Set<Long> blacklistFor(String symbol)               // все blacklist для символа
public boolean hasAnySubscriber()                          // есть ли хотя бы один подписчик
```

Метод `hasAnySubscriber()` используется в `TradeProcessor` для раннего выхода — если нет подписчиков, трейды не обрабатываются.

---

## 12. Скользящее окно (SlidingWindow)

Реализация **монотонных деков** (monotonic deque pattern) для amortized O(1) нахождения min/max.

```java
public class SlidingWindow {

    private final long windowLengthNs;
    private final Deque<TradePoint> minDeque = new ArrayDeque<>();
    private final Deque<TradePoint> maxDeque = new ArrayDeque<>();

    public SlidingWindow(long windowLengthNs) {
        this.windowLengthNs = windowLengthNs;
    }

    public synchronized void add(TradePoint newPoint) {
        long cutoff = newPoint.timestampNs() - windowLengthNs;

        // 1. Удаляем устаревшие точки (вне окна)
        while (!minDeque.isEmpty() && minDeque.getFirst().timestampNs() < cutoff)
            minDeque.removeFirst();
        while (!maxDeque.isEmpty() && maxDeque.getFirst().timestampNs() < cutoff)
            maxDeque.removeFirst();

        // 2. Поддерживаем монотонность для min (возрастающая)
        while (!minDeque.isEmpty() && minDeque.getLast().priceRaw() >= newPoint.priceRaw())
            minDeque.removeLast();
        minDeque.addLast(newPoint);

        // 3. Поддерживаем монотонность для max (убывающая)
        while (!maxDeque.isEmpty() && maxDeque.getLast().priceRaw() <= newPoint.priceRaw())
            maxDeque.removeLast();
        maxDeque.addLast(newPoint);
    }

    public synchronized long getMin() { ... }
    public synchronized long getMax() { ... }
    public synchronized boolean isUpMove() { ... }
    public synchronized boolean isEmpty() { ... }
    public synchronized double getCurrentImpulsePercent() { ... }
}
```

### Как работает monotonic deque

**Принцип:**

- `minDeque` хранит точки в **возрастающем** порядке цен → первый элемент = минимум в окне
- `maxDeque` хранит точки в **убывающем** порядке цен → первый элемент = максимум в окне

**При добавлении новой точки:**

1. Удаляются устаревшие точки с начала (вне окна)
2. С конца удаляются все «неконкурентные» точки (цена хуже новой)
3. Новая точка добавляется в конец

**Пример:**

```
Входящие цены: 100 → 150 → 120 → 200 → 180

minDeque после каждой:  [100] → [100] → [100,120] → [100,120,180] → [100,120,180]
maxDeque после каждой:  [100] → [150] → [150,120] → [200]         → [200,180]

min = 100, max = 200
```

**Потокобезопасность:** все методы `synchronized`. При 1 consumer thread contention отсутствует.

> **Примечание:** `getCurrentImpulsePercent()` возвращает положительное значение для UP-импульса, отрицательное для DOWN-импульса и `0.0` если min/max имеют одинаковый timestamp. Метод `isUpMove()` использует `>=` для сравнения timestamp'ов.

---

## 13. Domain — движок фильтрации

### WindowStore

Хранилище скользящих окон: `символ → timeWindowNs → WindowEntry → SlidingWindow`.

```java
@Component
public final class WindowStore {

    private static final long EVICTION_TTL_MS = 60_000L;  // 60 секунд

    private final Map<String, Map<Long, WindowEntry>> store = new ConcurrentHashMap<>();

    /**
     * Получить или создать окно. Выполняет lazy eviction мёртвых окон
     * перед созданием нового.
     */
    public SlidingWindow getOrComputeWindow(String symbol, long timeWindowNs) {
        var entries = store.computeIfAbsent(symbol, s -> new ConcurrentHashMap<>());

        // Lazy eviction при каждом обращении
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> e.getValue().isDead(now));

        var entry = entries.computeIfAbsent(timeWindowNs, WindowEntry::new);
        entry.touch();
        return entry.window();
    }

    /**
     * Получить все окна символа. Выполняет lazy eviction.
     */
    public Map<Long, SlidingWindow> getWindows(String symbol) {
        Map<Long, WindowEntry> entries = store.get(symbol);
        if (entries == null) return null;

        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> e.getValue().isDead(now));
        if (entries.isEmpty()) {
            store.remove(symbol);
            return null;
        }

        var result = new ConcurrentHashMap<Long, SlidingWindow>();
        for (var e : entries.entrySet())
            result.put(e.getKey(), e.getValue().window());
        return result;
    }

    private static final class WindowEntry {
        private final SlidingWindow window;
        private final AtomicLong lastWriteTimeMs = new AtomicLong();

        WindowEntry(long windowLengthNs) {
            this.window = new SlidingWindow(windowLengthNs);
            this.lastWriteTimeMs.set(System.currentTimeMillis());
        }

        SlidingWindow window() { return window; }
        void touch() { lastWriteTimeMs.set(System.currentTimeMillis()); }
        boolean isDead(long nowMs) { return nowMs - lastWriteTimeMs.get() > EVICTION_TTL_MS; }
    }
}
```

**Механизм eviction:**

- Каждое окно хранит `lastWriteTimeMs` — timestamp последней записи
- Eviction происходит **и** в `getOrComputeWindow()`, **и** в `getWindows()` — мёртвые окна удаляются при каждом обращении
- TTL = 60 секунд — если в окно не было записей > 60с, оно считается мёртвым
- Lazy-подход: eviction происходит без фоновых потоков, во время чтения

### FilterEngine

Движок проверки всех фильтров для трейда.

```java
@Component
public final class FilterEngine {

    public List<ImpulseFilterView> checkAll(
            String symbol,
            Map<Long, SlidingWindow> windows,
            Map<Long, Set<ImpulseFilterView>> filtersByWindow,
            Set<Long> blacklistedIds) {

        List<ImpulseFilterView> triggered = new ArrayList<>();

        for (Map.Entry<Long, Set<ImpulseFilterView>> entry : filtersByWindow.entrySet()) {
            long twNs = entry.getKey();
            SlidingWindow window = windows.get(twNs);
            if (window == null) continue;

            for (ImpulseFilterView filter : entry.getValue()) {
                // Blacklist: O(1) lookup
                if (blacklistedIds != null && blacklistedIds.contains(filter.filterId()))
                    continue;

                if (trigger(window, filter))
                    triggered.add(filter);
            }
        }
        return triggered;
    }

    private static boolean trigger(SlidingWindow window, ImpulseFilterView filter) {
        long min = window.getMin();
        long max = window.getMax();
        if (min == 0) return false;

        boolean isUp = window.isUpMove();
        double pct = filter.payload().percent();
        // Целочисленная проверка: max * 100 > min * (100 + percent)
        boolean amplitude = max * 100L > min * (100L + (long) pct);

        return switch (filter.payload().direction()) {
            case UP -> isUp && amplitude;
            case DOWN -> !isUp && amplitude;
            case BOTH -> amplitude;
        };
    }
}
```

**Логика `trigger()`:**

1. Берёт min и max цены из окна (O(1) через monotonic deque)
2. Проверяет амплитуду: `max * 100 > min * (100 + percent)` — целочисленная арифметика, без float
3. Проверяет направление: UP/DOWN/BOTH
4. Оба условия выполнены → фильтр сработал

---

## 14. Сервисы — оркестрация

### TradeProcessor

**Главный оркестратор** обработки трейдов.

```java
@Component
public final class TradeProcessor {

    private static final Logger log = LoggerFactory.getLogger(TradeProcessor.class);

    private final WindowStore windowStore;
    private final ImpulseTradesSection impulseTradesSection;
    private final FilterEngine filterEngine;
    private final AlertPublisher alertPublisher;

    public void process(String key, TradeEvent event) {
        String symbol = event.symbol();
        long ts = event.timestampNs();
        long price = event.price();

        // 0a. Нет активных фильтров → ранний выход
        Map<Long, Set<ImpulseFilterView>> filtersByWindow = impulseTradesSection.filtersByWindow();
        if (filtersByWindow.isEmpty()) return;

        // 0b. Нет подписчиков → не обрабатываем (экономия CPU)
        if (!impulseTradesSection.hasAnySubscriber()) return;

        // 1. Обновить ВСЕ окна для символа
        //    ОДНО окно на timeWindow — шардирование по размеру окна
        for (long twNs : filtersByWindow.keySet()) {
            windowStore.getOrComputeWindow(symbol, twNs).add(new TradePoint(ts, price));
        }

        // 2. Получить актуальные окна (с возможной eviction)
        Map<Long, SlidingWindow> windows = windowStore.getWindows(symbol);
        if (windows == null || windows.isEmpty()) return;

        // 3. Проверить все фильтры
        Set<Long> blacklistedIds = impulseTradesSection.blacklistFor(symbol);
        List<ImpulseFilterView> triggered = filterEngine.checkAll(
            symbol, windows, filtersByWindow, blacklistedIds);

        // 4. Для каждого сработавшего фильтра — отправить алерт
        for (ImpulseFilterView filter : triggered) {
            if (filter.subscribers().isEmpty()) continue;

            String k = filter.filterId() + ":" + symbol;
            AlertEvent alertEvent = new AlertEvent(
                filter.subscribers(),
                filter.payload().exchange(),
                filter.payload().market(),
                symbol,
                ts
            );

            alertPublisher.send(new ProducerRecord<>("alert-topic", k, alertEvent))
                .whenComplete((meta, ex) -> {
                    if (ex != null) {
                        log.error("ALERT send failed: filterId={}", filter.filterId(), ex);
                    } else {
                        log.debug("Alert sent: partition={} offset={}", meta.partition(), meta.offset());
                    }
                });
        }
    }
}
```

**Полный поток обработки:**

```
TradeEvent (Kafka: trades-topic)
  │
  ├─► early exit: filtersByWindow.isEmpty()
  ├─► early exit: hasAnySubscriber() == false
  │
  ├─► для каждого timeWindow:
  │     windowStore.getOrComputeWindow(symbol, twNs)
  │       └──► SlidingWindow.add(TradePoint)
  │             └──► eviction старых точек по времени
  │             └──► maintain monotonic deques
  │
  ├─► windowStore.getWindows(symbol)  ← с lazy eviction
  │
  ├─► FilterEngine.checkAll()
  │     └──► для каждого (timeWindow → набор фильтров):
  │           └──► trigger(window, filter)
  │                 └──► getMin(), getMax() — O(1)
  │                 └──► check amplitude: max*100 > min*(100+pct)
  │                 └──► check direction: UP/DOWN/BOTH
  │
  └──► для каждого triggered filter с подписчиками:
         └──► AlertPublisher.send(ProducerRecord<"alert-topic">)
               └──► [Kafka: alert-topic]
```

### ImpulseFilterEventHandler

Обработчик IMPULSE-событий из Kafka.

```java
@Component
public final class ImpulseFilterEventHandler implements FilterEventHandler {

    private final TradesStateStore tradesStateStore;

    @Override public String action() { return "IMPULSE"; }

    @Override
    public void apply(OutboxCreatedEvent event) {
        tradesStateStore.apply(event);
    }
}
```

**Полный поток filter-события:**

```
OutboxCreatedEvent (Kafka: filter-topic)
  │
  ▼
EventsListener.handle()
  │
  ▼
FilterEventHandlerRegistry.get("IMPULSE")  →  ImpulseFilterEventHandler
  │
  ▼
TradesStateStore.apply()
  │
  ▼
ImpulseTradesSection.apply(event)
  │
  ├── CREATE (synchronized):
  │     ├─ impulseFilters.put(key, state)
  │     ├─ filtersByWindow[timeWindow].add(view)
  │     └─ blacklist[symbol].add(filterId)
  │
  ├── DELETE (synchronized):
  │     ├─ impulseFilters.remove(key)
  │     ├─ filtersByWindow[timeWindow].remove(view)
  │     └─ blacklist[symbol].remove(filterId)
  │
  ├── SUBSCRIBE (lock-free, O(1)):
  │     ├─ state.subscribers.add(userId)  ← ConcurrentHashMap.newKeySet
  │     └─ updateFilterView(state, filterId)  ← обновляет ОДИН view
  │
  └── UNSUBSCRIBE (lock-free, O(1)):
        ├─ state.subscribers.remove(userId)
        └─ updateFilterView(state, filterId)  ← обновляет ОДИН view
              │
              ▼
        rebuildSnapshot()  ← пересобирает volatile snapshot
```

---

## 15. JSON-мапперы

Кастомные сериализаторы для `Direction` (числовой код ↔ enum).

### DirectionJsonReader

```java
@Component
public final class DirectionJsonReader implements JsonReader<Direction> {
    @Override
    public Direction read(JsonParser parser) throws IOException {
        int code = parser.getIntValue();
        return Direction.fromCode((short) code);
    }
}
```

### DirectionJsonWriter

```java
@Component
public final class DirectionJsonWriter implements JsonWriter<Direction> {
    @Override
    public void write(JsonGenerator gen, Direction value) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.code);
        }
    }
}
```

---

## 16. Полный поток данных

### Поток trade-события

```
[Kafka: trades-topic]
        │
        ▼
  TradesListener.handle(key, TradeEvent)
        │
        ▼
  TradeProcessor.process()
        │
        ├──► early exit: filtersByWindow.isEmpty()
        │
        ├──► early exit: !hasAnySubscriber()
        │
        ├──► для каждого timeWindow:
        │       windowStore.getOrComputeWindow(symbol, twNs)
        │           └──► SlidingWindow.add(TradePoint)
        │                   └──► eviction старых точек
        │                   └──► maintain monotonic deques
        │
        ├──► windowStore.getWindows(symbol)  ← с lazy eviction
        │
        ├──► FilterEngine.checkAll()
        │       └──► для каждого (timeWindow → набор фильтров):
        │               └──► trigger(window, filter)
        │                       └──► getMin(), getMax() — O(1)
        │                       └──► check amplitude: max*100 > min*(100+pct)
        │                       └──► check direction: UP/DOWN/BOTH
        │
        └──► для каждого triggered filter с подписчиками:
                └──► AlertPublisher.send(ProducerRecord<"alert-topic">)
                        └──► [Kafka: alert-topic]
```

### Поток filter-события

См. раздел [ImpulseFilterEventHandler](#impulsefiltereventhandler).

---

## 17. Конкурентная модель

### Синхронизация

| Компонент | Что защищено | Механизм | Обоснование |
|---|---|---|---|
| **SlidingWindow** | Все методы | `synchronized` | 1 consumer thread — contention отсутствует |
| **ImpulseTradesSection.createFilter** | Модификация структур | `synchronized` | CREATE меняет 3 структуры атомарно |
| **ImpulseTradesSection.deleteFilter** | Модификация структур | `synchronized` | DELETE меняет 3 структуры атомарно |
| **ImpulseTradesSection.subscribe** | Подписчики | lock-free | `ConcurrentHashMap.newKeySet()` — потокобезопасный |
| **ImpulseTradesSection.unsubscribe** | Подписчики | lock-free | `ConcurrentHashMap.newKeySet()` — потокобезопасный |
| **WindowStore** | ConcurrentHashMap | `computeIfAbsent` | Lock-free lazy init |
| **WindowStore eviction** | `removeIf` на entrySet | ConcurrentHashMap-safe | Вызывается из consumer thread |

### Оптимизации

| Оптимизация | Описание | Выигрыш |
|---|---|---|
| **subscribe/unsubscribe O(1)** | Обновление одного view вместо полной пересборки | В N раз быстрее при большом N |
| **hasAnySubscriber()** | Ранний выход если нет подписчиков | Экономия CPU при отсутствии подписчиков |
| **filtersByWindow.isEmpty()** | Ранний выход если нет фильтров | Экономия CPU |
| **Группировка по timeWindow** | Одно SlidingWindow на все фильтры с одинаковым окном | N фильтров → 1 запись в окно |
| **Monotonic deque** | Amortized O(1) min/max | Вместо O(N) сканирования |

### Группировка по timeWindow

**Ключевая оптимизация:** фильтры с одинаковым `timeWindow` используют **одно** `SlidingWindow`.

```
Фильтры:  F1(5s, 5%),  F2(5s, 4%),  F3(10s, 3%)

Окна:     SlidingWindow(5s)  →  используется F1 и F2
          SlidingWindow(10s) →  используется F3

На каждый трейд:  2 записи в окна (не 3)
                  1 проход по windows (не по каждому фильтру отдельно)
```

---

## 18. Тестирование

### Структура тестов

| Тест | Файл | Что проверяет |
|---|---|---|
| **DirectionTest** (11 тестов) | `common/util/DirectionTest.java` | Все значения enum, `fromCode()`, ordinal, case sensitivity |
| **OutboxOperationTest** (10 тестов) | `common/util/OutboxOperationTest.java` | Все значения enum, `fromCode()`, ordinal |
| **OutboxTopicResolverTest** (6 тестов) | `common/util/OutboxTopicResolverTest.java` | Маппинг action → topic, null/blank/unknown |
| **FilterKeyTest** (5 тестов) | `core/FilterKeyTest.java` | `equals()`, `hashCode()`, `toString()` record |
| **TradePointTest** (8 тестов) | `core/trades/TradePointTest.java` | `equals()`, `hashCode()`, большие значения, нули |
| **ImpulseFilterStateTest** (8 тестов) | `core/trades/impulse/ImpulseFilterStateTest.java` | Добавление/удаление подписчиков, payload, дубликаты |
| **ImpulseFilterViewTest** (3 теста) | `core/trades/impulse/ImpulseFilterViewTest.java` | Поля record, `equals()`, `hashCode()` |
| **ImpulseTradesSectionTest** (10 тестов) | `core/trades/impulse/ImpulseTradesSectionTest.java` | Полный CRUD-цикл, `filtersByWindow`, blacklist |
| **FilterEngineTest** (10 тестов) | `domain/FilterEngineTest.java` | Trigger UP/DOWN/BOTH, порог, blacklist, разные time windows |
| **SlidingWindowTest** (10 тестов) | `util/SlidingWindowTest.java` | Empty, single point, up/down move, eviction, cutoff boundary |
| **TradeProcessorIntegrationTest** (11 тестов) | `services/TradeProcessorIntegrationTest.java` | **Интеграционный**: полный поток trade → window → filter → alert с моком |

### TradeProcessorIntegrationTest — сценарии

| Сценарий | Описание |
|---|---|
| `noFilter_noAlert` | Нет фильтров → алертов нет |
| `trade_triggersOnImpulse` | Цена превысила порог → алерт отправлен |
| `trade_belowThreshold_noAlert` | Цена ниже порога → алертов нет |
| `trade_blacklistedSymbol_noAlert` | Символ в blacklist → алертов нет |
| `trade_noSubscribe_noAlert` | Фильтр есть, но нет подписчиков → алертов нет |
| `trade_multipleSubscribers_oneRecord` | Несколько подписчиков → все в одном алерте |
| `trade_twoFilters_sameWindow_bothTrigger` | Два фильтра на одно окно → оба сработали |
| `trade_twoFilters_differentWindows_onlyLowerTriggers` | Разные time windows → корректная группировка |
| `trade_downMove_triggersDownFilter` | Падение цены → trigger для DOWN |
| `trade_downMove_noTriggerOnUpFilter` | Падение не триггерит UP-фильтр |
| `trade_bothDirections_upAndDown_bothTrigger` | Любое направление → trigger для BOTH |

### Запуск тестов

```bash
./gradlew :data-processor:test
```

Все **92 теста** проходят.

---

## 19. Запуск

### Локально

```bash
# Требуется переменная окружения KAFKA_BOOTSTRAP_SERVERS
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
./gradlew :data-processor:run
```

### Fat JAR

```bash
./gradlew :data-processor:fatJar
java -jar data-processor/build/libs/data-processor-1.0-SNAPSHOT-fat.jar
```

### Docker

```bash
docker build -t data-processor -f data-processor/Dockerfile .
docker run -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 -p 8082:8082 -p 8087:8087 data-processor
```

### Через docker-compose (из корня проекта)

```bash
docker-compose up data-processor
```

---

## 20. Известные ограничения и TODO

### 1. Конвертация секунд в наносекунды

```java
long twNs = payload.timeWindow() * 1_000_000_000L;
```

`timeWindow` приходит в секундах (int), конвертируется в наносекунды. При `timeWindow > 2147483647` (≈68 лет) произойдёт переполнение — на практике недостижимо.

### 2. Один тип фильтра — IMPULSE

Текущая реализация поддерживает только импульсные фильтры. Логика `trigger()` захардкожена в `FilterEngine`.

**TODO:** вынести `trigger()` в стратегию (`FilterStrategy`) для поддержки новых типов фильтров (wavelet, crossover и т.д.).

### 3. Поле `volume24h` не используется

Поле определено в `OutboxPayload.ImpulseFilter`, но не участвует в логике обработки. Зарезервировано для будущего функционала фильтрации по объёму.

### 4. Целочисленный `percent`

`percent` — `int`, минимальный порог = 1%. Для субпроцентных импульсов (0.1%, 0.25%) потребуется изменение на `double` или basis points.

### 5. Single-threaded consumer

`threads = 1` для обоих consumer'ов. TradeProcessor — CPU-bound операция. Для масштабирования > 10-20K events/sec потребуется:
- Увеличение `kafka.TradesConsumer.threads`
- Предварительное исправление thread-safety в subscribe/unsubscribe (уже частично решено через `ConcurrentHashMap.newKeySet()`)
- Возможно — шардирование по partition

### 6. Нет metrics/observability

Метрики (количество трейдов, задержка, размер окон, алерты sent/failed) не собираются. Health endpoint доступен (`/health` на порту 8082), но не содержит детальной информации.

### 7. Нет alert cooldown/dedup

При устойчивом импульсе фильтр может срабатывать на **каждый** трейд, отправляя дубликаты алертов. Cooldown-механизм не реализован.

### 8. Нет retry для failed alerts

Ошибки отправки алерта логируются, но алерт не повторяется. Для production рекомендуется добавить retry с exponential backoff или dead letter queue.

### 9. application.conf и application-docker.conf идентичны

Оба файла содержат одинаковый контент с `${KAFKA_BOOTSTRAP_SERVERS}`. В идеале `application.conf` должен содержать дефолтные значения (например, `localhost:9092`).

---

## 21. Расширяемость

### Добавление нового типа фильтра

Архитектура позволяет добавить новый тип фильтра (например, `WAVELET` или `CROSSOVER`) без изменения существующего кода — **Open/Closed Principle**:

**Шаг 1.** Создать новый `record` в `OutboxPayload`:

```java
@JsonDiscriminatorValue("WAVELET")
record WaveletFilter(
    List<String> exchange, List<String> market,
    int timeWindow, double threshold
) implements OutboxPayload {}
```

**Шаг 2.** Реализовать `TradesFilterSection`:

```java
@Component
public final class WaveletTradesSection implements TradesFilterSection {
    @Override public String action() { return "WAVELET"; }
    @Override public void apply(OutboxCreatedEvent event) { ... }
}
```

**Шаг 3.** Реализовать `FilterEventHandler`:

```java
@Component
public final class WaveletFilterEventHandler implements FilterEventHandler {
    @Override public String action() { return "WAVELET"; }
    @Override public void apply(OutboxCreatedEvent event) { tradesStateStore.apply(event); }
}
```

Kora автоматически зарегистрирует компоненты через `All<>`.

### Диаграмма зависимостей (DI)

```
TradeProcessor
  ├── WindowStore
  ├── ImpulseTradesSection
  ├── FilterEngine
  └── AlertPublisher

FilterEventHandlerRegistry
  └── All<FilterEventHandler>
        └── ImpulseFilterEventHandler
              └── TradesStateStore
                    └── All<TradesFilterSection>
                          └── ImpulseTradesSection

TradesListener
  └── TradeProcessor

EventsListener
  └── FilterEventHandlerRegistry
```

### HTTP-порты

Порты зарезервированы, REST-контроллеры в модуле отсутствуют:

- **8082** (publicApi) — health check (`/health`), future metrics
- **8087** (privateApi) — внутренние эндпоинты
