package ru.core.cache;

import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.models.domain.FilterKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ru.models.states.ImpulseFilterState;
import ru.models.states.ImpulseFilterView;
import ru.tinkoff.kora.common.Component;

@Component
public final class ImpulseTradesSection implements TradesFilterSection {

    private final Map<FilterKey, ImpulseFilterState> impulseFilters = new HashMap<>();
    private volatile List<ImpulseFilterView> activeImpulseSnapshot = List.of();
    /** TimeWindowNs → Set<Filter>. Фильтры сгруппированы по времени окна. */
    private final Map<Long, Set<ImpulseFilterView>> filtersByWindow = new ConcurrentHashMap<>();
    /** Symbol → Set<FilterId>. Какие фильтры ИСКЛЮЧАЮТ этот символ. */
    private final Map<String, Set<Long>> blacklist = new ConcurrentHashMap<>();

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
        rebuildSnapshot();
    }

    private synchronized void createFilter(OutboxCreatedEvent event) {
        FilterKey key = new FilterKey(event.action(), event.filterId());
        var payload = (OutboxPayload.ImpulseFilter) event.payload();
        var state = new ImpulseFilterState(payload);

        impulseFilters.put(key, state);

        assert payload != null;
        long twNs = payload.timeWindow() * 1_000_000_000L;
        ImpulseFilterView view = toView(state, key.filterId());
        filtersByWindow.computeIfAbsent(twNs, t -> ConcurrentHashMap.newKeySet()).add(view);

        if (payload.blackList() != null) {
            for (String symbol : payload.blackList()) {
                blacklist.computeIfAbsent(symbol, s -> ConcurrentHashMap.newKeySet()).add(key.filterId());
            }
        }
    }

    private synchronized void deleteFilter(OutboxCreatedEvent event) {
        FilterKey key = new FilterKey(event.action(), event.filterId());
        var state = impulseFilters.remove(key);
        if (state == null) return;

        long twNs = state.payload().timeWindow() * 1_000_000_000L;
        Set<ImpulseFilterView> set = filtersByWindow.get(twNs);
        if (set != null) {
            set.removeIf(f -> f.filterId() == key.filterId());
            if (set.isEmpty()) filtersByWindow.remove(twNs);
        }

        List<String> bl = state.payload().blackList();
        if (bl != null) {
            for (String symbol : bl) {
                Set<Long> set2 = blacklist.get(symbol);
                if (set2 != null) {
                    set2.remove(key.filterId());
                    if (set2.isEmpty()) blacklist.remove(symbol);
                }
            }
        }
    }

    private void subscribe(OutboxCreatedEvent event) {
        FilterKey key = new FilterKey(event.action(), event.filterId());
        var state = impulseFilters.get(key);
        if (state != null) {
            state.subscribers().add(event.userId());
            updateFilterView(state, key.filterId());
        }
    }

    private void unsubscribe(OutboxCreatedEvent event) {
        FilterKey key = new FilterKey(event.action(), event.filterId());
        var state = impulseFilters.get(key);
        if (state != null) {
            state.subscribers().remove(event.userId());
            updateFilterView(state, key.filterId());
        }
    }

    /** Обновить view ОДНОГО фильтра — O(1) вместо полной пересборки O(N). */
    private void updateFilterView(ImpulseFilterState state, long filterId) {
        long twNs = state.payload().timeWindow() * 1_000_000_000L;
        Set<ImpulseFilterView> set = filtersByWindow.get(twNs);
        if (set != null) {
            set.removeIf(f -> f.filterId() == filterId);
            set.add(toView(state, filterId));
        }
    }

    /** Пересобрать snapshot (для обратной совместимости). */
    private void rebuildSnapshot() {
        var list = new ArrayList<ImpulseFilterView>(impulseFilters.size());
        for (Map.Entry<FilterKey, ImpulseFilterState> e : impulseFilters.entrySet()) {
            list.add(toView(e.getValue(), e.getKey().filterId()));
        }
        activeImpulseSnapshot = List.copyOf(list);
    }

    private ImpulseFilterView toView(ImpulseFilterState state, long filterId) {
        return new ImpulseFilterView(filterId, state.payload(), Set.copyOf(state.subscribers()));
    }

    public List<ImpulseFilterView> activeImpulseFilters() {
        return activeImpulseSnapshot;
    }

    /** Все фильтры, сгруппированные по timeWindow (ns). Для onTrade — без копирования. */
    public Map<Long, Set<ImpulseFilterView>> filtersByWindow() {
        return filtersByWindow;
    }

    /** Проверить: фильтр filterId ИСКЛЮЧАЕТ этот символ? */
    public boolean isBlacklisted(String symbol, long filterId) {
        Set<Long> bl = blacklist.get(symbol);
        return bl != null && bl.contains(filterId);
    }

    public Set<Long> blacklistFor(String symbol) {
        return blacklist.get(symbol);
    }

    /** Есть ли хотя бы один фильтр с подписчиками? Для раннего выхода в hot path. */
    public boolean hasAnySubscriber() {
        for (var state : impulseFilters.values()) {
            if (!state.subscribers().isEmpty()) return true;
        }
        return false;
    }
}
