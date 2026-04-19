package ru.core.storage;

import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.core.cache.FilterStore;
import ru.core.cache.Index;
import ru.core.cache.Storage;
import ru.models.states.ImpulseFilterView;
import ru.tinkoff.kora.common.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class StorageImpl implements Storage {

    private final Index index;
    private final FilterStore filterStore;

    public StorageImpl(Index index, FilterStore filterStore) {
        this.index = index;
        this.filterStore = filterStore;
    }

    @Override
    public String action() {
        return "IMPULSE";
    }

    @Override
    public void apply(OutboxCreatedEvent event) {
        switch (event.operation()) {
            case CREATE -> createFilter(event);
            case DELETE -> deleteFilter(event);
            case SUBSCRIBE -> subscribe(event);
            case UNSUBSCRIBE -> unsubscribe(event);
        }
    }

    private void subscribe(OutboxCreatedEvent event) {
        ImpulseFilterView old = filterStore.get(event.filterId());
        if (old == null) return;

        Set<Long> newSubs = new HashSet<>(old.subscribers());
        newSubs.add(event.userId());

        filterStore.put(event.filterId(),
                new ImpulseFilterView(old.filterId(), old.payload(), newSubs));
    }

    private void unsubscribe(OutboxCreatedEvent event) {
        long fid = event.filterId();
        long uid = event.userId();
        ImpulseFilterView old = filterStore.get(fid);
        if (old == null) return;

        Set<Long> newSubs = new HashSet<>(old.subscribers());
        newSubs.remove(uid);

        filterStore.put(fid,
                new ImpulseFilterView(old.filterId(), old.payload(), newSubs));
    }

    // TODO может быть несогласновано
    private void deleteFilter(OutboxCreatedEvent event) {
        long id = event.filterId();
        filterStore.remove(id);
        index.delete(id);
    }

    private void createFilter(OutboxCreatedEvent event) {

        var payload = (OutboxPayload.ImpulseFilter) event.payload();
        assert payload != null;

        ImpulseFilterView view = new ImpulseFilterView(
                event.filterId(),
                payload,
                ConcurrentHashMap.newKeySet()
        );

        filterStore.put(event.filterId(), view);

        index.create(
                event.filterId(),
                payload.blackList(),
                payload.timeWindow() * 1_000_000_000L
        );
    }
}
