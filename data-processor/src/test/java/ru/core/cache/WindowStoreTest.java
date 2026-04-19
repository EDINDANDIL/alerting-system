package ru.core.cache;

import org.junit.jupiter.api.Test;
import ru.core.util.SlidingWindow;

import static org.junit.jupiter.api.Assertions.*;

class WindowStoreTest {

    private final WindowStore store = new WindowStore();

    private static final String SYMBOL = "ETH";

    @Test
    void getOrCompute_createsNewWindow() {
        long twNs = 60L * 1_000_000_000L;
        SlidingWindow w = store.getOrCompute(SYMBOL, twNs);
        assertNotNull(w);
    }

    @Test
    void getOrCompute_sameTimeWindow_sameInstance() {
        long twNs = 60L * 1_000_000_000L;
        var w1 = store.getOrCompute(SYMBOL, twNs);
        var w2 = store.getOrCompute(SYMBOL, twNs);
        assertSame(w1, w2);
    }

    @Test
    void getOrCompute_differentTimeWindows_differentWindows() {
        var w5s = store.getOrCompute(SYMBOL, 5_000_000_000L);
        var w60s = store.getOrCompute(SYMBOL, 60_000_000_000L);
        assertNotSame(w5s, w60s);
    }

    @Test
    void get_returnsExistingWindow() {
        long twNs = 60L * 1_000_000_000L;
        store.getOrCompute(SYMBOL, twNs);

        var w = store.get(SYMBOL, twNs);
        assertTrue(w.isPresent());
    }

    @Test
    void get_returnsEmptyForUnknownWindow() {
        assertTrue(store.get(SYMBOL, 999L).isEmpty());
    }

    @Test
    void multipleWindows_independentLifecycle() {
        long tw1 = 5_000_000_000L;
        long tw2 = 60_000_000_000L;
        store.getOrCompute(SYMBOL, tw1);
        store.getOrCompute(SYMBOL, tw2);

        assertTrue(store.get(SYMBOL, tw1).isPresent());
        assertTrue(store.get(SYMBOL, tw2).isPresent());
    }
}
