package ru.common.persistence.entity;

import jakarta.annotation.Nullable;
import ru.common.dto.OutboxPayload;
import ru.common.mappers.outbox.OutboxOperationParameterColumnMapper;
import ru.common.mappers.outbox.OutboxOperationResultColumnMapper;
import ru.common.util.OutboxOperation;
import ru.tinkoff.kora.common.Mapping;
import ru.tinkoff.kora.database.common.annotation.Column;
import ru.tinkoff.kora.database.common.annotation.Id;
import ru.tinkoff.kora.database.jdbc.EntityJdbc;
import ru.tinkoff.kora.json.common.annotation.Json;

import java.time.OffsetDateTime;

@EntityJdbc
public record FilterOutboxEntity(

        @Id
        @Column("id")
        Long eventId,

        @Column("action")
        String action,

        @Mapping(OutboxOperationResultColumnMapper.class)
        @Mapping(OutboxOperationParameterColumnMapper.class)
        @Column("operation")
        OutboxOperation operation,

        @Column("filter_id")
        Long filterId,

        @Column("user_id")
        Integer userId,

        @Json
        @Column("payload")
        @Nullable OutboxPayload payload,

        @Column("created_at")
        OffsetDateTime createdAt
) {}