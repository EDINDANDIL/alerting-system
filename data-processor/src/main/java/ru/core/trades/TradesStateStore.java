package ru.core.trades;

import ru.common.dto.OutboxCreatedEvent;
import ru.core.trades.impulse.ImpulseFilterView;
import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.common.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public final class TradesStateStore {
    private final Map<String, TradesFilterSection> sections;
    public TradesStateStore(All<TradesFilterSection> sections) {
        this.sections = sections.stream()
                .collect(Collectors.toMap(TradesFilterSection::action, s -> s));
    }
    public void apply(OutboxCreatedEvent event) {
        TradesFilterSection section = sections.get(event.action());
        if (section == null) {
            throw new IllegalArgumentException("Unsupported Trades action: " + event.action());
        }
        section.apply(event);
    }
}