package ru.common.dto;

import ru.common.util.Direction;
import ru.tinkoff.kora.json.common.annotation.*;
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
            int timeWindow,
            Direction direction,
            int percent,
            int volume24h
    ) implements OutboxPayload {}
}