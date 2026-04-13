package ru.models.states;

import ru.common.dto.OutboxPayload;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// мутабельное состояние хранилища для удобного изменения
public final class ImpulseFilterState {

    private final OutboxPayload.ImpulseFilter payload;
    private final Set<Integer> subscribers = ConcurrentHashMap.newKeySet();

    public ImpulseFilterState(OutboxPayload.ImpulseFilter payload) {
        this.payload = payload;
    }

    public OutboxPayload.ImpulseFilter payload() {
        return payload;
    }

    public Set<Integer> subscribers() {
        return subscribers;
    }
}

