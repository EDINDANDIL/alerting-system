package ru.flink.models;

import ru.common.dto.OutboxPayload;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public record ImpulseRuntimeFilter(
        long filterId,
        OutboxPayload.ImpulseFilter payload,
        Set<Long> subscribers
) implements Serializable {

    public ImpulseRuntimeFilter subscribe(long userId) {
        Set<Long> next = new HashSet<>(subscribers);
        next.add(userId);
        return new ImpulseRuntimeFilter(filterId, payload, Set.copyOf(next));
    }

    public ImpulseRuntimeFilter unsubscribe(long userId) {
        Set<Long> next = new HashSet<>(subscribers);
        next.remove(userId);
        return new ImpulseRuntimeFilter(filterId, payload, Set.copyOf(next));
    }
}