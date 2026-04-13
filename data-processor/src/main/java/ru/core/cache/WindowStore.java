package ru.core.cache;

import ru.core.util.SlidingWindow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ru.tinkoff.kora.common.Component;

/**
 * Хранит скользящие окна: символ → timeWindow → WindowEntry.
 * Lazy creation — окно создаётся при первом трейде для пары (symbol, timeWindow).
 * Автоматическая очистка: окна, в которые не было записей дольше TTL, удаляются.
 */
@Component
public final class WindowStore {

    /** Окна, в которые не было записей дольше этого времени (мс), считаются мёртвыми. */
    private static final long EVICTION_TTL_MS = 60_000L;

    private final Map<String, Map<Long, WindowEntry>> store = new ConcurrentHashMap<>();

    /**
     * Все окна для символа.
     * @return null если окон нет (caller должен проверить).
     */
    public Map<Long, SlidingWindow> getWindows(String symbol) {
        Map<Long, WindowEntry> entries = store.get(symbol);
        if (entries == null) return null;

        // Lazy eviction при чтении
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> e.getValue().isDead(now));
        if (entries.isEmpty()) {
            store.remove(symbol);
            return null;
        }

        // Вернуть map только SlidingWindow
        var result = new ConcurrentHashMap<Long, SlidingWindow>();
        for (var e : entries.entrySet()) {
            result.put(e.getKey(), e.getValue().window());
        }
        return result;
    }

    /**
     * Получить или создать окно для пары (symbol, timeWindowNs).
     * Перед созданием выполняет lazy eviction мёртвых окон.
     */
    public SlidingWindow getOrComputeWindow(String symbol, long timeWindowNs) {
        var entries = store.computeIfAbsent(symbol, s -> new ConcurrentHashMap<>());

        // Lazy eviction перед созданием нового окна
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> e.getValue().isDead(now));

        var entry = entries.computeIfAbsent(timeWindowNs, WindowEntry::new);
        entry.touch();
        return entry.window();
    }

    /**
     * Обёртка над SlidingWindow с timestamp последней записи.
     */
    private static final class WindowEntry {
        private final SlidingWindow window;
        private final AtomicLong lastWriteTimeMs = new AtomicLong();

        WindowEntry(long windowLengthNs) {
            this.window = new SlidingWindow(windowLengthNs);
            this.lastWriteTimeMs.set(System.currentTimeMillis());
        }

        SlidingWindow window() {
            return window;
        }

        void touch() {
            lastWriteTimeMs.set(System.currentTimeMillis());
        }

        boolean isDead(long nowMs) {
            return nowMs - lastWriteTimeMs.get() > EVICTION_TTL_MS;
        }
    }
}