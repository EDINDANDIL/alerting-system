package ru.core.cache;

import ru.core.engine.FilterEngine;
import ru.core.util.MonetStore;
import ru.core.util.SlidingWindow;
import ru.models.domain.TradePoint;
import ru.models.dto.TradeEvent;
import ru.models.states.ImpulseFilterView;
import ru.tinkoff.kora.common.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Координатор обработки трейдов:
 *   addPoint → обновить окна
 *   check    → проверить фильтры
 *   create   → зарегистрировать фильтр для символов
 *   delete   → убрать фильтр отовсюду
 */
@Component
public final class Index {

    private final MonetStore store;
    private final WindowStore windowStore;
    private final FilterStore section;
    private final FilterEngine filterEngine;

    private final Map<String, Map<Long, Set<Long>>> index = new ConcurrentHashMap<>();

    public Index(MonetStore store, WindowStore windowStore,
                 FilterStore section, FilterEngine filterEngine) {
        this.store = store;
        this.windowStore = windowStore;
        this.section = section;
        this.filterEngine = filterEngine;
    }

    public void addPoint(TradeEvent event) {
        Map<Long, Set<Long>> windows = index.get(event.symbol());
        if (windows == null) return;
        TradePoint point = new TradePoint(event.timestampNs(), event.price());
        windows.keySet().forEach(
        timeNs -> windowStore.get(timeNs).ifPresent(w -> w.add(point))
        );
    }

    public List<ImpulseFilterView> check(String symbol) {
        Map<Long, Set<Long>> windows = index.get(symbol);
        if (windows == null) return List.of();

        List<ImpulseFilterView> result = new ArrayList<>();
        for (var entry : windows.entrySet()) {
            long twNs = entry.getKey();
            Set<Long> filterIds = entry.getValue();
            if (filterIds.isEmpty()) continue;

            SlidingWindow w = windowStore.get(twNs).orElse(null);
            if (w == null) continue;

            List<ImpulseFilterView> filters = section.getAll(filterIds);
            if (filters.isEmpty()) continue;

            result.addAll(filterEngine.checkAll(w, filters));
        }
        return result;
    }

    public Map<String, Map<Long, Set<Long>>> create(long id, Set<String> blacklist, long time) {

        Set<String> allowed = store.getSymbols()
            .stream()
            .filter(s -> blacklist == null || !blacklist.contains(s))
            .collect(Collectors.toSet());

        allowed.forEach(
    s -> index.computeIfAbsent(s, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(time, t -> ConcurrentHashMap.newKeySet())
            .add(id));

        return index;
    }

    public void delete(long id) {
        index.forEach((symbol, windows) ->
                windows.forEach((time, ids) -> ids.remove(id))
        );
        index.values().forEach(windows ->
                windows.values().removeIf(Set::isEmpty)
        );
        index.values().removeIf(Map::isEmpty);
    }
}
