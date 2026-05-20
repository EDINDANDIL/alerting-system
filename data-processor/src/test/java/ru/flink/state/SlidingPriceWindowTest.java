package ru.flink.state;

import org.junit.jupiter.api.Test;
import ru.flink.models.TradePoint;

import static org.junit.jupiter.api.Assertions.*;

class SlidingPriceWindowTest {

    private static final long WINDOW_NS = 1_000L;

    @Test
    void emptyWindow_returnsZeroMinMaxAndNoUpMove() {
        SlidingPriceWindow window = new SlidingPriceWindow(WINDOW_NS);

        assertEquals(0L, window.min());
        assertEquals(0L, window.max());
        assertFalse(window.isUpMove());
    }

    @Test
    void add_tracksMinAndMax() {
        SlidingPriceWindow window = new SlidingPriceWindow(WINDOW_NS);

        window.add(point(100L, 100L));
        window.add(point(200L, 80L));
        window.add(point(300L, 120L));

        assertEquals(80L, window.min());
        assertEquals(120L, window.max());
    }

    @Test
    void add_evictsPointsOutsideWindow() {
        SlidingPriceWindow window = new SlidingPriceWindow(100L);

        window.add(point(0L, 50L));
        window.add(point(50L, 80L));
        window.add(point(151L, 60L));

        assertEquals(60L, window.min());
        assertEquals(60L, window.max());
    }

    //TODO посмотреть тесты
    @Test
    void add_keepsPointOnWindowBoundary() {
        SlidingPriceWindow window = new SlidingPriceWindow(100L);

        window.add(point(0L, 50L));
        window.add(point(100L, 80L));

        assertEquals(50L, window.min());
        assertEquals(80L, window.max());
    }

    //TODO посмотреть тесты
    @Test
    void add_evictsExpiredMinButKeepsValidMax() {
        SlidingPriceWindow window = new SlidingPriceWindow(100L);

        window.add(point(0L, 50L));
        window.add(point(50L, 120L));
        window.add(point(101L, 90L));

        assertEquals(90L, window.min());
        assertEquals(120L, window.max());
    }

    //TODO посмотреть тесты
    @Test
    void add_evictsExpiredMaxButKeepsValidMin() {
        SlidingPriceWindow window = new SlidingPriceWindow(100L);

        window.add(point(0L, 120L));
        window.add(point(50L, 50L));
        window.add(point(101L, 90L));

        assertEquals(50L, window.min());
        assertEquals(90L, window.max());
    }

    @Test
    void isUpMove_returnsTrueWhenMaxIsNotEarlierThanMin() {
        SlidingPriceWindow window = new SlidingPriceWindow(WINDOW_NS);

        window.add(point(100L, 100L));
        window.add(point(200L, 120L));

        assertTrue(window.isUpMove());
    }

    @Test
    void isUpMove_returnsFalseWhenMaxIsEarlierThanMin() {
        SlidingPriceWindow window = new SlidingPriceWindow(WINDOW_NS);

        window.add(point(100L, 120L));
        window.add(point(200L, 100L));

        assertFalse(window.isUpMove());
    }

    //TODO посмотреть тесты
    @Test
    void isUpMove_returnsTrueForFlatMoveBecauseLatestEqualPriceIsBothMinAndMax() {
        SlidingPriceWindow window = new SlidingPriceWindow(WINDOW_NS);

        window.add(point(100L, 100L));
        window.add(point(200L, 100L));

        assertTrue(window.isUpMove());
    }

    private static TradePoint point(long timestampNs, long priceRaw) {
        return new TradePoint(timestampNs, priceRaw);
    }
}
