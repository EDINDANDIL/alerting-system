package ru.persistence.entity;

import ru.tinkoff.kora.database.common.annotation.Column;
import ru.tinkoff.kora.database.common.annotation.Table;
import ru.tinkoff.kora.database.jdbc.EntityJdbc;

import java.time.OffsetDateTime;

@EntityJdbc
@Table("user_impulse_filters")
public record UserImpulseFilterEntity(
        @Column("user_id")
        Long userId,
        @Column("impulse_id")
        Long impulseId,
        @Column("created_at")
        OffsetDateTime createdAt
) {}
