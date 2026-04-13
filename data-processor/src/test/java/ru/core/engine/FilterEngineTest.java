package ru.core.engine;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.core.util.SlidingWindow;
import ru.models.domain.TradePoint;
import ru.models.states.ImpulseFilterView;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilterEngineTest {

    private final FilterEngine engine = new FilterEngine();

    @Test
    void upMove_aboveThreshold_triggers() {
        SlidingWindow window = windowWith(120L);
        ImpulseFilterView filter = impulseView(1L, Direction.UP, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertEquals(1, result.size());
    }

    @Test
    void upMove_belowThreshold_noTrigger() {
        var window = windowWith(105L);
        var filter = impulseView(1L, Direction.UP, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void upMove_exactlyAtThreshold_noTrigger() {
        // max * 100 > min * (100 + pct) → 110 * 100 = 11000, 100 * 110 = 11000 → 11000 > 11000 = false
        var window = windowWith(110L);
        var filter = impulseView(1L, Direction.UP, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void downMove_aboveThreshold_triggers() {
        var window = windowWith(80L);
        var filter = impulseView(1L, Direction.DOWN, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertEquals(1, result.size());
    }

    @Test
    void downMove_belowThreshold_noTrigger() {
        var window = windowWith(95L);
        var filter = impulseView(1L, Direction.DOWN, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void bothMove_largeUp_triggers() {
        var window = windowWith(120L);
        var filter = impulseView(1L, Direction.BOTH, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertEquals(1, result.size());
    }

    @Test
    void bothMove_largeDown_triggers() {
        var window = windowWith(80L);
        var filter = impulseView(1L, Direction.BOTH, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertEquals(1, result.size());
    }

    @Test
    void bothMove_smallChange_noTrigger() {
        var window = windowWith(105L);
        var filter = impulseView(1L, Direction.BOTH, 10);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void blacklistedFilter_skipped() {
        var window = windowWith(200L);
        var filter = impulseView(1L, Direction.UP, 5);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(filter)), Set.of(1L));
        assertTrue(result.isEmpty());
    }

    @Test
    void nonBlacklistedFilter_triggers() {
        var window = windowWith(200L);
        var f1 = impulseView(1L, Direction.UP, 5);
        var f2 = impulseView(2L, Direction.UP, 5);
        var result = checkAll(window, Map.of(1_000_000_000L, Set.of(f1, f2)), Set.of(1L));
        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().filterId());
    }

    @Test
    void filtersWithDifferentWindows_independent() {
        var window5s = windowWith(120L);
        var window60s = windowWith(105L);

        var f5 = impulseView(1L, Direction.UP, 10);
        var f60 = impulseView(2L, Direction.UP, 10);

        var windows = Map.of(5_000_000_000L, window5s, 60_000_000_000L, window60s);
        var filters = Map.of(5_000_000_000L, Set.of(f5), 60_000_000_000L, Set.of(f60));

        var result = engine.checkAll("btcusdt", windows, filters, null);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().filterId());
    }

    @Test
    void emptyFilters_returnsEmpty() {
        var window = windowWith(120L);
        var result = checkAll(window, Map.of(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void nullWindow_returnsNoResults() {
        var filter = impulseView(1L, Direction.UP, 10);
        var result = engine.checkAll("btcusdt", Map.of(), Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void zeroMinPrice_noTrigger() {
        SlidingWindow w = new SlidingWindow(1_000_000_000L);
        w.add(new TradePoint(1_000L, 0L));
        w.add(new TradePoint(2_000L, 100L));
        var filter = impulseView(1L, Direction.UP, 5);
        var result = checkAll(w, Map.of(1_000_000_000L, Set.of(filter)), null);
        assertTrue(result.isEmpty());
    }

    private SlidingWindow windowWith(long price2) {
        SlidingWindow w = new SlidingWindow(1_000_000_000L);
        w.add(new TradePoint(1_000L, 100L));
        w.add(new TradePoint(2_000L, price2));
        return w;
    }

    private ImpulseFilterView impulseView(long filterId, Direction direction, int percent) {
        var payload = new OutboxPayload.ImpulseFilter(
                List.of("binance"), List.of("futures"), List.of(),
                60, direction, percent, 0);
        return new ImpulseFilterView(filterId, payload, Set.of(100));
    }

    private List<ImpulseFilterView> checkAll(
            SlidingWindow window,
            Map<Long, Set<ImpulseFilterView>> filters,
            Set<Long> blacklisted) {
        return engine.checkAll("btcusdt", Map.of(1_000_000_000L, window), filters, blacklisted);
    }
}
