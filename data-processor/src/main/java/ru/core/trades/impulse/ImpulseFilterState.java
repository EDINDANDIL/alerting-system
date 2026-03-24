package ru.core.trades.impulse;

import ru.common.dto.OutboxPayload;

import java.util.HashSet;
import java.util.Set;

// мутабельное состояние хранилища для удобного изменения
public final class ImpulseFilterState {

    private final OutboxPayload.ImpulseFilter payload;
    private final Set<Integer> subscribers = new HashSet<>();

    ImpulseFilterState(OutboxPayload.ImpulseFilter payload) {
        this.payload = payload;
    }

    OutboxPayload.ImpulseFilter payload() {
        return payload;
    }

    Set<Integer> subscribers() {
        return subscribers;
    }
}

