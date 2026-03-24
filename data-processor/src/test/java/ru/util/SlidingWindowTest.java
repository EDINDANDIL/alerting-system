package ru.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.core.trades.TradePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlidingWindowTest {

    private static final long WINDOW_NS = 1_000_000_000L; // 1s

    @Test
    void emptyWindow_returnsZeroImpulse() {
        var w = new SlidingWindow(WINDOW_NS);
        Assertions.assertEquals(0.0, w.getCurrentImpulsePercent());
    }

    @Test
    void singlePoint_returnsZeroImpulse() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1000L, 50_000L));
        Assertions.assertEquals(0.0, w.getCurrentImpulsePercent());
    }

    @Test
    void twoPoints_sameTimestamp_returnsZeroImpulse() {
        var w = new SlidingWindow(WINDOW_NS);
        long t = 5_000L;
        w.add(tp(t, 10L));
        w.add(tp(t, 20L));
        Assertions.assertEquals(0.0, w.getCurrentImpulsePercent());
    }

    @Test
    void upMove_laterHigherPrice_returnsPositiveRatio() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1_000L, 100L));
        w.add(tp(2_000L, 125L));
        Assertions.assertEquals(0.25, w.getCurrentImpulsePercent(), 1e-9);
    }

    @Test
    void downMove_earlierHighLaterLow_returnsNegativeRatio() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1_000L, 100L));
        w.add(tp(2_000L, 80L));
        Assertions.assertEquals(-0.25, w.getCurrentImpulsePercent(), 1e-9);
    }

    @Test
    void minPriceZero_returnsZero() {
        var w = new SlidingWindow(WINDOW_NS);
        w.add(tp(1_000L, 0L));
        w.add(tp(2_000L, 100L));
        Assertions.assertEquals(0.0, w.getCurrentImpulsePercent());
    }

    @Test
    void evictsPointsStrictlyOlderThanWindow_keepsRecentMinMax() {
        long win = 1_000L;
        var w = new SlidingWindow(win);

        w.add(tp(0L, 10L));
        w.add(tp(500L, 50L));
        w.add(tp(2_000L, 40L));

        assertEquals(1, w.maxDeque.size());
        assertEquals(1, w.minDeque.size());

        // Может возникнуть лаг и min/max придется заменить на текущее, поэтому динамика скидывается
        assertEquals(0, w.getCurrentImpulsePercent());
    }

    @Test
    void pointExactlyAtCutoff_isNotEvicted() {
        long win = 1_000L;
        var w = new SlidingWindow(win);

        w.add(tp(0L, 100L));
        w.add(tp(1_000L, 150L));

        Assertions.assertEquals(0.5, w.getCurrentImpulsePercent(), 1e-9);
    }

    @Test
    void sequenceOfAdds_updatesImpulseAfterEviction() {
        long win = 10_000L;
        var w = new SlidingWindow(win);

        w.add(tp(0L, 100L));
        w.add(tp(5_000L, 120L));
        Assertions.assertEquals(0.2, w.getCurrentImpulsePercent(), 1e-9);

        w.add(tp(12_000L, 130L));

        double impulse = w.getCurrentImpulsePercent();
        Assertions.assertTrue(impulse > 0);
        Assertions.assertTrue(impulse < 0.2);
    }

    private static TradePoint tp(long timestampNs, long priceRaw) {
        return new TradePoint(timestampNs, priceRaw);
    }
}