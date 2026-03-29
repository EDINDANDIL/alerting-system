# Build stage
FROM gradle:9.1.0-jdk25 AS builder

WORKDIR /app

# Копируем весь проект для корректной сборки
COPY . .

# Собираем только нужный модуль и fat jar
RUN gradle :data-processor:fatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Создаём пользователя для безопасности
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Копируем fat jar из builder
COPY --from=builder --chown=appuser:appgroup /app/data-processor/build/libs/*-fat.jar app.jar

# Переключаемся на не-root пользователя
USER appuser

# Порт сервиса
EXPOSE 8082 8087

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/health || exit 1

# Переменная окружения для выбора конфига (docker или local)
ENV KORA_CONFIG_APPLICATION=application-docker.conf

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
