package ru.common.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorField;
import ru.tinkoff.kora.json.common.annotation.JsonDiscriminatorValue;

import java.util.Set;

@Json
@JsonDiscriminatorField("action")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OutboxPayload.ImpulseFilter.class, name = "IMPULSE")
})
public sealed interface OutboxPayload {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilter(
            Set<String> exchange,
            Set<String> market,
            Set<String> blackList,
            long timeWindow,
            Direction direction,
            long percent,
            long volume24h
    ) implements OutboxPayload {}
}
