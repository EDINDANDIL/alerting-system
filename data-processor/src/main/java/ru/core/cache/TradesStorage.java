package ru.core.cache;

import ru.common.dto.OutboxCreatedEvent;
import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.common.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public final class TradesStorage {
    private final Map<String, Storage> sections;

    public TradesStorage(All<Storage> sections) {
        this.sections = sections.stream()
                .collect(Collectors.toMap(Storage::action, s -> s));
    }

    public void apply(OutboxCreatedEvent event) {
        Storage section = sections.get(event.action());
        if (section == null) {
            throw new IllegalArgumentException("Unsupported Trades action: " + event.action());
        }
        section.apply(event);
    }
}