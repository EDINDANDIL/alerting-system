package ru.services.handlers;

import ru.common.dto.OutboxCreatedEvent;
import ru.core.cache.TradesStorage;
import ru.tinkoff.kora.common.Component;

@Component
public final class ImpulseFilterEventHandler implements FilterEventHandler {

    private final TradesStorage tradesStorage;

    public ImpulseFilterEventHandler(TradesStorage tradesStorage) {
        this.tradesStorage = tradesStorage;
    }

    @Override
    public String action() {
        return "IMPULSE";
    }

    @Override
    public void apply(OutboxCreatedEvent event) {
        tradesStorage.apply(event);
    }
}