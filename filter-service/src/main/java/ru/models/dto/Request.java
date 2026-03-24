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
            int timeWindow,
            Direction direction,
            int percent,
            int volume24h
    ) implements Request {}
}
