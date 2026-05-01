package ru.persistence.entity;

import java.util.List;
import ru.tinkoff.kora.database.common.annotation.Column;
import ru.tinkoff.kora.database.common.annotation.Id;
import ru.tinkoff.kora.database.common.annotation.Table;
import ru.tinkoff.kora.database.jdbc.EntityJdbc;
import ru.common.util.Direction;

@EntityJdbc
@Table("impulse_filters")
public record ImpulseFilterEntity(
        @Id
        @Column("id")
        Long id,

        @Column("exchanges")
        List<String> exchange,

        @Column("markets")
        List<String> market,

        @Column("blacklist")
        List<String> blackList,

        @Column("action")
        String action,

        @Column("time_window_sec")
        Integer timeWindow,

        @Column("direction_code")
        Direction direction,

        @Column("percent")
        Integer percent,

        @Column("volume_24h")
        Long volume24h //TODO
) {}