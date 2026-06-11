package ru.models.dto;

import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorField;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorValue;
import ru.tinkoff.kora.json.common.annotation.JsonWriter;

import java.util.List;

@JsonWriter
@JsonDiscriminatorField("action")
public sealed interface Response {

    @JsonWriter
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilterResponse(
            long id,
            List<String> exchange,
            List<String> market,
            List<String> blackList,
            String action,
            long timeWindow,
            Direction direction,
            int percent,
            long volume24h
    ) implements Response {}

    @Json
    record AuthResponse(String token) {}
}