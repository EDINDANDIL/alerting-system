package ru.core.cache;

import ru.common.dto.OutboxCreatedEvent;

public interface Storage {
    String action();
    void apply(OutboxCreatedEvent event);
}