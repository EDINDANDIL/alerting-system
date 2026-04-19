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

    /** Глобальная таблица окон {symbol : timeWindowNs → SlidingWindow} */
    private final Map<String, Map<Long, SlidingWindow>> table = new ConcurrentHashMap<>();

    public WindowStore() {}

    /**
     * Получить окно по timeWindowNs.
     */
    public Optional<SlidingWindow> get(String symbol, long timeWindowNs) {
        return Optional.ofNullable(table.get(symbol))
        .map(m -> m.get(timeWindowNs));
        // Это map от Optional, а не от stream, не путать
    }

    /**
     * Получить или создать окно для конкретного символа и временного окна.
     */
    public Optional<SlidingWindow> getOrCompute(String symbol, long timeWindowNs) {
        return Optional.of(
                table.computeIfAbsent(symbol, _ -> new ConcurrentHashMap<>())
                        .computeIfAbsent(timeWindowNs, SlidingWindow::new)
        );
    }

    /**
     * Удалить все окна для символа (если больше нет фильтров).
     */
    public void removeSymbol(String symbol) {
        table.remove(symbol);
    }
}
