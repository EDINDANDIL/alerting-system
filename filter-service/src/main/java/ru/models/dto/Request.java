package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorField;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorValue;
import ru.common.util.Direction;

import java.util.List;

@Json
@JsonDiscriminatorField("action")
public sealed interface Request {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilterDto(
            List<String> exchange,
            List<String> market,
            List<String> blackList,
            String action,
            long timeWindow,
            Direction direction,
            int percent,
            long volume24h
    ) implements Request {
        public ImpulseFilterDto {
            if (blackList == null) blackList = List.of();
            if (exchange == null) exchange = List.of();
            if (market == null) market = List.of();
        }
    }

    @Json
    record AuthRequest(String email, String password) {}
}