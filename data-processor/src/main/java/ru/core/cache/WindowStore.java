package ru.core.cache;

import ru.core.util.SlidingWindow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ru.tinkoff.kora.common.Component;

/**
 * Хранит глобальные SlidingWindow: timeWindowNs → SlidingWindow.
 * Создаёт окна лениво при первом обращении.
 */
@Component
public final class WindowStore {

    /** Глобальная таблица окон: timeWindowNs → SlidingWindow */
    private final Map<Long, SlidingWindow> table = new ConcurrentHashMap<>();

    public WindowStore() {
    }

    /**
     * Получить окно по timeWindowNs.
     */
    public Optional<SlidingWindow> get(Long time) {
        return Optional.ofNullable(table.get(time));
    }

    /**
     * Получить или создать окно.
     */
    public SlidingWindow getOrCompute(long timeWindowNs) {
        return table.computeIfAbsent(timeWindowNs, SlidingWindow::new);
    }
}
