package ru.models.dto;

import jakarta.annotation.Nullable;
import ru.common.dto.OutboxPayload;
import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record AlertEvent(
        long filterId,
        int userId,
        @Nullable OutboxPayload payload
) {};
