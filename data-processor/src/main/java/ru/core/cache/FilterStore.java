package ru.core.cache;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ru.models.states.ImpulseFilterView;
import ru.tinkoff.kora.common.Component;

@Component
public final class FilterStore {

    public final Map<Long, ImpulseFilterView> filterViewMap = new ConcurrentHashMap<>();

    public List<ImpulseFilterView> getAll(Set<Long> ids) {
        return ids.stream()
            .map(filterViewMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public boolean hasAnySubscriber() {
        for (ImpulseFilterView fv : filterViewMap.values()) {
            if (!fv.subscribers().isEmpty()) return true;
        }
        return false;
    }

    public void put(long id, ImpulseFilterView impulseFilterView) {
        filterViewMap.put(id, impulseFilterView);
    }

    public ImpulseFilterView get(long id) {
        return filterViewMap.get(id);
    }

    public void remove(long id) {
        filterViewMap.remove(id);
    }
}
