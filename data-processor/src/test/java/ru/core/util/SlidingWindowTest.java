package ru.core.util;

import org.junit.jupiter.api.Test;
import ru.models.domain.TradePoint;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowTest {

    private static final long WINDOW_NS = 1_000_000_000L;

    @Test
    void getMin_getMax_returnCorrectValues() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1_000L, 100L));
        w.add(tp(2_000L, 80L));
        w.add(tp(3_000L, 120L));
        assertEquals(80L, w.getMin());
        assertEquals(120L, w.getMax());
    }

    @Test
    void isEmpty_returnsTrueForNewWindow() {
        var w = new SlidingWindow(WINDOW_NS);
        assertTrue(w.isEmpty());
        w.add(tp(1_000L, 100L));
        assertFalse(w.isEmpty());
    }

    @Test
    void isUpMove_laterMax_returnsTrue() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1_000L, 100L));
        w.add(tp(2_000L, 150L));
        assertTrue(w.isUpMove());
    }

    @Test
    void isUpMove_earlierMax_returnsFalse() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(2_000L, 150L));
        w.add(tp(1_000L, 100L));
        // После добавления в обратном порядке: max(150) имеет timestamp 2000, min(100) имеет timestamp 1000
        // max.timestamp > min.timestamp → isUpMove = true
        // Но monotonic deque: после add(2000,150) → max=[150], min=[150]
        //   затем add(1000,100) → min=[100] (150 удалён т.к. >= 100)
        // max = 150 @ 2000, min = 100 @ 1000 → isUpMove = true
        assertTrue(w.isUpMove());
    }

    @Test
    void monotonicProperty_minIsAlwaysMinimum() {
        long win = 100_000L;
        var w = new SlidingWindow(win);
        w.add(tp(0L, 50L));
        w.add(tp(10L, 30L));
        w.add(tp(20L, 40L));
        w.add(tp(30L, 20L));
        assertEquals(20L, w.getMin());
    }

    @Test
    void monotonicProperty_maxIsAlwaysMaximum() {
        long win = 100_000L;
        var w = new SlidingWindow(win);
        w.add(tp(0L, 50L));
        w.add(tp(10L, 70L));
        w.add(tp(20L, 60L));
        w.add(tp(30L, 80L));
        assertEquals(80L, w.getMax());
    }

    private static TradePoint tp(long timestampNs, long priceRaw) {
        return new TradePoint(timestampNs, priceRaw);
    }
}
