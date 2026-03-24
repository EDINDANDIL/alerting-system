package ru.core;

import ru.common.dto.OutboxCreatedEvent;

public interface FilterEventHandler {
    String action();
    void apply(OutboxCreatedEvent event);
}
