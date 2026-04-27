package ru.flink.state;

import org.junit.jupiter.api.Test;
import ru.flink.model.TradePoint;

import static org.junit.jupiter.api.Assertions.*;

class PriceWindowTest {

    private static final long WINDOW_NS = 1_000L;

    @Test
    void emptyWindow_returnsZeroMinMaxAndNoUpMove() {
        PriceWindow window = new PriceWindow(WINDOW_NS);

        assertEquals(0L, window.min());
        assertEquals(0L, window.max());
        assertFalse(window.isUpMove());
    }

    @Test
    void add_tracksMinAndMax() {
        PriceWindow window = new PriceWindow(WINDOW_NS);

        window.add(point(100L, 100L));
        window.add(point(200L, 80L));
        window.add(point(300L, 120L));

        assertEquals(80L, window.min());
        assertEquals(120L, window.max());
    }

    @Test
    void add_evictsPointsOutsideWindow() {
        PriceWindow window = new PriceWindow(100L);

        window.add(point(0L, 50L));
        window.add(point(50L, 80L));
        window.add(point(151L, 60L));

        assertEquals(60L, window.min());
        assertEquals(60L, window.max());
    }

    @Test
    void isUpMove_returnsTrueWhenMaxIsNotEarlierThanMin() {
        PriceWindow window = new PriceWindow(WINDOW_NS);

        window.add(point(100L, 100L));
        window.add(point(200L, 120L));

        assertTrue(window.isUpMove());
    }

    @Test
    void isUpMove_returnsFalseWhenMaxIsEarlierThanMin() {
        PriceWindow window = new PriceWindow(WINDOW_NS);

        window.add(point(100L, 120L));
        window.add(point(200L, 100L));

        assertFalse(window.isUpMove());
    }

    private static TradePoint point(long timestampNs, long priceRaw) {
        return new TradePoint(timestampNs, priceRaw);
    }
}
