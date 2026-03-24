package ru.core.trades;

import ru.common.dto.OutboxCreatedEvent;

public interface TradesFilterSection {
    String action();
    void apply(OutboxCreatedEvent event);
}