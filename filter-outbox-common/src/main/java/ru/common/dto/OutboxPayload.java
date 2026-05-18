package ru.common.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.common.util.Direction;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OutboxPayload.ImpulseFilter.class, name = "IMPULSE")
})
public sealed interface OutboxPayload {

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