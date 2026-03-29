# Tiger Desktop Alert Client

Клиентское приложение для получения алертов об импульсах от Alert Service.

## Требования

- Java 25+
- Запущенный Alert Service (порт 7818)

## Быстрый старт

### 1. Запуск из IDE

Запустите главный класс:
```
ru.desktop.TigerDesktopClient
```

Аргументы (опционально):
- `userId` — ваш идентификатор пользователя (по умолчанию: 123)

### 2. Запуск из командной строки

```bash
# Сборка JAR
gradlew :client-desktop:jar

# Запуск
java -jar client-desktop/build/libs/client-desktop-1.0.jar [userId]

# Пример
java -jar client-desktop/build/libs/client-desktop-1.0.jar 456
```

## Как это работает

1. Клиент подключается к Alert Service по WebSocket:
   ```
   ws://localhost:7818/ws?userId=<YOUR_ID>
   ```

2. При обнаружении импульса сервер отправляет алерт:
   ```json
   {"subscribers":[123],"exchange":["BINANCE"],"market":["SPOT"],"symbol":"BTCUSDT","timestampNs":1234567890000000000}
   ```

3. Клиент показывает:
   - Консольное уведомление
   - Системное уведомление (в трее, если поддерживается)

## Функции

### ✅ Поддерживаемые функции

- Подключение к Alert Service
- Получение алертов в реальном времени
- Автоматическое переподключение при разрыве
- Поддержание соединения (ping/pong каждые 30 сек)
- Системные уведомления (Windows/macOS/Linux)
- Graceful shutdown

### 📋 Формат алертов

См. [PROTOCOL.md](PROTOCOL.md) для подробного описания протокола.

## Пример вывода

```
╔══════════════════════════════════════════════════════════╗
║          Tiger Desktop Alert Client                     ║
╠══════════════════════════════════════════════════════════╣
║ User ID: 123
║ Server:  ws://localhost:7818/ws?userId=123
╚══════════════════════════════════════════════════════════╝

✅ Connected to alert server as user 123
📨 ALERT received: {"subscribers":[123],"exchange":["BINANCE"],"market":["SPOT"],"symbol":"BTCUSDT","timestampNs":1234567890000000000}
🔔 IMPULSE ALERT!
Symbol: BTCUSDT
Exchange: BINANCE
Market: SPOT
Time: 1234567890 ms
```

## Конфигурация

### Передача userId

```bash
# Через аргумент командной строки
java -jar client-desktop-1.0.jar 789

# В IDE: укажите аргумент программы "789"
```

## Устранение неполадок

### Ошибка: "Connection refused"

**Причина:** Alert Service не запущен.

**Решение:**
```bash
# Запустите alert-service
gradlew :alert-service:run
```

### Ошибка: "WebSocket error"

**Причина:** Неправильный URL или порт.

**Решение:** Проверьте, что подключаетесь к `ws://localhost:7818/ws?userId=<ID>`

### Системные уведомления не работают

**Причина:** System Tray не поддерживается на вашей системе.

**Решение:** Алерты будут показываться только в консоли.

### Частые разрывы соединения

**Причина:** Таймаут бездействия.

**Решение:** Клиент автоматически отправляет ping каждые 30 секунд. Если проблема сохраняется, проверьте сетевые настройки.

## Интеграция с Alert Service

### Настройка Alert Service

Убедитесь, что в `alert-service/src/main/resources/application.conf` указано:

```hocon
alert {
  ws {
    port = 7818
  }
}
```

### Проверка подключения

1. Запустите Alert Service
2. Запустите TigerDesktopClient
3. В логе Alert Service должно появиться:
   ```
   User 123 connected. onlineUsers=1, channels=1
   ```

## Разработка

### Структура проекта

```
client-desktop/
├── src/main/java/ru/desktop/
│   ├── TigerDesktopClient.java    # Основной клиент
│   └── ...
├── build.gradle                    # Конфигурация сборки
└── PROTOCOL.md                     # Документация протокола
```

### Зависимости

- `Java-WebSocket` — WebSocket клиент
- `Jackson` — JSON парсинг
- `SLF4J` — Логирование

### Сборка

```bash
# Очистка и сборка
gradlew :client-desktop:clean :client-desktop:build

# Только JAR
gradlew :client-desktop:jar
```

## Лицензия

Внутренний проект Tiger Trading.

---

**Версия:** 1.0  
**Дата:** 2026-03-29
