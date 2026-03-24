package ru.services.trades.impulse;

import ru.common.dto.OutboxCreatedEvent;
import ru.core.FilterEventHandler;
import ru.core.trades.TradesStateStore;
import ru.tinkoff.kora.common.Component;

@Component
public final class ImpulseFilterEventHandler implements FilterEventHandler {

    private final TradesStateStore tradesStateStore;

    public ImpulseFilterEventHandler(TradesStateStore tradesStateStore) {
        this.tradesStateStore = tradesStateStore;
    }

    @Override
    public String action() {
        return "IMPULSE";
    }

    @Override
    public void apply(OutboxCreatedEvent event) {
        tradesStateStore.apply(event);
    }
}