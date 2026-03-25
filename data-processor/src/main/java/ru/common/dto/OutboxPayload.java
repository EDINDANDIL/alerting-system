package ru.common.dto;

import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorField;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorValue;

import java.util.List;

@Json
@JsonDiscriminatorField("action")
public sealed interface OutboxPayload {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilter(
            List<String> exchange,
            List<String> market,
            List<String> blackList,
            int timeWindowNs,
            Direction direction,
            int percent,
            int volume24h
    ) implements OutboxPayload {}
}