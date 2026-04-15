package ru.core.cache;

import org.junit.jupiter.api.Test;
import ru.core.util.SlidingWindow;

import static org.junit.jupiter.api.Assertions.*;

class WindowStoreTest {

    private final WindowStore store = new WindowStore();

    @Test
    void getOrCompute_createsNewWindow() {
        long twNs = 60L * 1_000_000_000L;
        SlidingWindow w = store.getOrCompute(twNs);
        assertNotNull(w);
    }

    @Test
    void getOrCompute_sameTimeWindow_sameInstance() {
        long twNs = 60L * 1_000_000_000L;
        var w1 = store.getOrCompute(twNs);
        var w2 = store.getOrCompute(twNs);
        assertSame(w1, w2);
    }

    @Test
    void getOrCompute_differentTimeWindows_differentWindows() {
        var w5s = store.getOrCompute(5_000_000_000L);
        var w60s = store.getOrCompute(60_000_000_000L);
        assertNotSame(w5s, w60s);
    }

    @Test
    void get_returnsExistingWindow() {
        long twNs = 60L * 1_000_000_000L;
        store.getOrCompute(twNs);

        var w = store.get(twNs);
        assertTrue(w.isPresent());
    }

    @Test
    void get_returnsEmptyForUnknownWindow() {
        assertTrue(store.get(999L).isEmpty());
    }

    @Test
    void multipleWindows_independentLifecycle() {
        long tw1 = 5_000_000_000L;
        long tw2 = 60_000_000_000L;
        store.getOrCompute(tw1);
        store.getOrCompute(tw2);

        assertTrue(store.get(tw1).isPresent());
        assertTrue(store.get(tw2).isPresent());
    }
}
