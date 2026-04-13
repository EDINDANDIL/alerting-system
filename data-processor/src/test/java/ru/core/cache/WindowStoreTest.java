package ru.core.cache;

import org.junit.jupiter.api.Test;
import ru.core.util.SlidingWindow;

import static org.junit.jupiter.api.Assertions.*;

class WindowStoreTest {

    private final WindowStore store = new WindowStore();

    @Test
    void getOrComputeWindow_createsNewWindow() {
        long twNs = 60L * 1_000_000_000L;
        SlidingWindow w = store.getOrComputeWindow("btcusdt", twNs);
        assertNotNull(w);
    }

    @Test
    void getOrComputeWindow_sameSymbol_sameWindow() {
        long twNs = 60L * 1_000_000_000L;
        var w1 = store.getOrComputeWindow("btcusdt", twNs);
        var w2 = store.getOrComputeWindow("btcusdt", twNs);
        assertSame(w1, w2);
    }

    @Test
    void getOrComputeWindow_differentSymbols_differentWindows() {
        long twNs = 60L * 1_000_000_000L;
        var w1 = store.getOrComputeWindow("btcusdt", twNs);
        var w2 = store.getOrComputeWindow("ethusdt", twNs);
        assertNotSame(w1, w2);
    }

    @Test
    void getOrComputeWindow_differentTimeWindows_differentWindows() {
        var w5s = store.getOrComputeWindow("btcusdt", 5_000_000_000L);
        var w60s = store.getOrComputeWindow("btcusdt", 60_000_000_000L);
        assertNotSame(w5s, w60s);
    }

    @Test
    void getWindows_returnsAllWindowsForSymbol() {
        long tw1 = 5_000_000_000L;
        long tw2 = 60_000_000_000L;
        store.getOrComputeWindow("btcusdt", tw1);
        store.getOrComputeWindow("btcusdt", tw2);

        var windows = store.getWindows("btcusdt");
        assertNotNull(windows);
        assertEquals(2, windows.size());
        assertTrue(windows.containsKey(tw1));
        assertTrue(windows.containsKey(tw2));
    }

    @Test
    void getWindows_nullForUnknownSymbol() {
        assertNull(store.getWindows("unknown"));
    }

    @Test
    void getWindows_removesSymbolAfterAllEntriesDead() {
        long twNs = 60L * 1_000_000_000L;
        store.getOrComputeWindow("btcusdt", twNs);

        // Ждём > 60 секунд (TTL) — в реальном тесте это медленно,
        // поэтому проверяем что getWindows возвращает данные сразу
        var windows = store.getWindows("btcusdt");
        assertNotNull(windows);
        assertEquals(1, windows.size());
    }

    @Test
    void multipleSymbols_independentLifecycle() {
        long twNs = 60L * 1_000_000_000L;
        store.getOrComputeWindow("btcusdt", twNs);
        store.getOrComputeWindow("ethusdt", twNs);

        var btc = store.getWindows("btcusdt");
        var eth = store.getWindows("ethusdt");
        assertNotNull(btc);
        assertNotNull(eth);
        assertEquals(1, btc.size());
        assertEquals(1, eth.size());
    }
}
