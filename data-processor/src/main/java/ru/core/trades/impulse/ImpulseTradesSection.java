package ru.core.trades.impulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.core.FilterKey;
import ru.core.trades.TradesFilterSection;
import ru.tinkoff.kora.common.Component;

import java.util.*;

@Component
public final class ImpulseTradesSection implements TradesFilterSection {
    @Override
    public String action() {
        return "IMPULSE";
    }

    private final Map<FilterKey, ImpulseFilterState> impulseFilters = new HashMap<>();
    private volatile List<ImpulseFilterView> activeImpulseSnapshot = List.of();
    private static final Logger log = LoggerFactory.getLogger(ImpulseTradesSection.class);

    @Override
    public synchronized void apply(OutboxCreatedEvent event) {
        FilterKey key = new FilterKey(event.action(), event.filterId());

        log.info("snapshot before {}", activeImpulseSnapshot);

        switch (event.operation()) {
            case CREATE -> applyCreate(key, event);
            case SUBSCRIBE -> applySubscribe(key, event);
            case UNSUBSCRIBE -> applyUnsubscribe(key, event);
            case DELETE -> applyDelete(key);
            default -> throw new UnsupportedOperationException("Unsupported operation: " + event.operation());
        }
        rebuildSnapshot();

        log.info("snapshot after {}", activeImpulseSnapshot);
    }

    // Снимает коллекцию активных, согласованных конфигураций на текущий момент
    public List<ImpulseFilterView> activeImpulseFilters() {
        return activeImpulseSnapshot;
    }

    public ImpulseFilterState get(FilterKey key) {
        return impulseFilters.get(key);
    }

    public void applyCreate(FilterKey key, OutboxCreatedEvent event) {
        if (!(event.payload() instanceof OutboxPayload.ImpulseFilter payload)) {
            throw new IllegalArgumentException("CREATE IMPULSE must contain ImpulseFilter payload");
        }
        impulseFilters.put(key, new ImpulseFilterState(payload));
    }

    private void applySubscribe(FilterKey key, OutboxCreatedEvent event) {
        ImpulseFilterState state = impulseFilters.get(key);
        if (state == null) return; // TODO log.warn()
        state.subscribers().add(event.userId());
    }

    private void applyUnsubscribe(FilterKey key, OutboxCreatedEvent event) {
        ImpulseFilterState state = impulseFilters.get(key);
        if (state == null) return; // TODO log.warn()
        state.subscribers().remove(event.userId());
    }

    private void applyDelete(FilterKey key) {
        impulseFilters.remove(key);
    }

    private void rebuildSnapshot() {
        activeImpulseSnapshot = impulseFilters.entrySet().stream()
                .filter(e -> !e.getValue().subscribers().isEmpty())
                .map(e -> new ImpulseFilterView(
                        e.getKey().filterId(),
                        e.getValue().payload(),
                        Set.copyOf(e.getValue().subscribers())
                ))
                .toList();
    }
}