package ru.core.engine;

import ru.core.util.SlidingWindow;
import ru.models.states.ImpulseFilterView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.tinkoff.kora.common.Component;

/**
 * Проверяет ВСЕ фильтры для трейда.
 * Делегирует trigger() конкретной стратегии (пока только impulse).
 */
@Component
public final class FilterEngine {

    /**
     * Проверить все фильтры для данного трейда.
     *
     * @param symbol        символ монеты
     * @param windows       окна для этого символа: timeWindowNs → SlidingWindow
     * @param filtersByWindow все фильтры: timeWindowNs → List<filter>
     * @param blacklistedIds filterId которые ИСКЛЮЧАЮТ этот символ
     * @return список сработавших фильтров
     */
    public List<ImpulseFilterView> checkAll(
            String symbol,
            Map<Long, SlidingWindow> windows,
            Map<Long, Set<ImpulseFilterView>> filtersByWindow,
            Set<Long> blacklistedIds) {

        List<ImpulseFilterView> triggered = new ArrayList<>();

        for (Map.Entry<Long, Set<ImpulseFilterView>> entry : filtersByWindow.entrySet()) {
            long twNs = entry.getKey();
            SlidingWindow window = windows.get(twNs);
            if (window == null) continue; // нет окна для этого timeWindow

            for (ImpulseFilterView filter : entry.getValue()) {
                if (blacklistedIds != null && blacklistedIds.contains(filter.filterId())) continue;
                if (trigger(window, filter)) triggered.add(filter);
            }
        }
        return triggered;
    }

    /**
     * Проверить фильтры для одного окна.
     * Используется из Index.check() — одно окно, набор фильтров.
     */
    public List<ImpulseFilterView> checkAll(SlidingWindow window, List<ImpulseFilterView> filters) {
        List<ImpulseFilterView> triggered = new ArrayList<>();
        for (ImpulseFilterView filter : filters) {
            if (trigger(window, filter)) triggered.add(filter);
        }
        return triggered;
    }

    // TODO: вынести в FilterStrategy когда появится wavelet/crossover
    private static boolean trigger(SlidingWindow window, ImpulseFilterView filter) {
        long min = window.getMin();
        long max = window.getMax();
        if (min == 0) return false;

        boolean isUp = window.isUpMove();
        double pct = filter.payload().percent();
        boolean amplitude = max * 100L > min * (100L + (long) pct);

        return switch (filter.payload().direction()) {
            case UP -> isUp && amplitude;
            case DOWN -> !isUp && amplitude;
            case BOTH -> amplitude;
        };
    }
}
