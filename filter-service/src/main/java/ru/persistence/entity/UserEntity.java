package ru.persistence.entity;

import ru.tinkoff.kora.database.common.annotation.Column;
import ru.tinkoff.kora.database.common.annotation.Table;

import ru.tinkoff.kora.database.jdbc.EntityJdbc;

@EntityJdbc
@Table("users")
public record UserEntity(
        @Column("id") Long id,
        @Column("email") String email,
        @Column("password_hash") String passwordHash
) {}