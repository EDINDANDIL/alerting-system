package ru.flink.model;

import ru.common.dto.OutboxPayload;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public record RuntimeFilter(
        long filterId,
        OutboxPayload.ImpulseFilter payload,
        Set<Long> subscribers
) implements Serializable {

    public RuntimeFilter subscribe(long userId) {
        Set<Long> next = new HashSet<>(subscribers);
        next.add(userId);
        return new RuntimeFilter(filterId, payload, Set.copyOf(next));
    }

    public RuntimeFilter unsubscribe(long userId) {
        Set<Long> next = new HashSet<>(subscribers);
        next.remove(userId);
        return new RuntimeFilter(filterId, payload, Set.copyOf(next));
    }
}