package ru.common.dto;

import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorField;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorValue;

import java.util.List;
import java.util.Set;

@Json
@JsonDiscriminatorField("action")
public sealed interface OutboxPayload {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilter(
            Set<String> exchange,
            Set<String> market,
            Set<String> blackList,
            int timeWindow,
            Direction direction,
            int percent,
            int volume24h
    ) implements OutboxPayload {}
}