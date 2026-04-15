package ru.common.dto;

import jakarta.annotation.Nullable;
import ru.common.util.OutboxOperation;
import ru.tinkoff.kora.json.common.annotation.Json;

import java.time.OffsetDateTime;

@Json
public record OutboxCreatedEvent(
        String action,
        OutboxOperation operation,
        long filterId,
        long userId,
        OffsetDateTime createdAt,
        @Nullable OutboxPayload payload
) {}