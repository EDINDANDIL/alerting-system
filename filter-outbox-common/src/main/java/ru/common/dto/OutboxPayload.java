package ru.common.dto;

import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.*;
import java.util.List;
import java.util.Set;

@Json
@JsonDiscriminatorField("action")
public sealed interface OutboxPayload {

    @Json
    @JsonDiscriminatorValue("IMPULSE")
    record ImpulseFilter(
            List<String> exchange,
            List<String> market,
            Set<String> blackList,
            int timeWindow,
            Direction direction,
            int percent,
            int volume24h
    ) implements OutboxPayload {}
}