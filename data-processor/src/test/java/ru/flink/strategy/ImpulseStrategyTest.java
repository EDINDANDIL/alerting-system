package ru.flink.strategy;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.flink.models.ImpulseRuntimeFilter;
import ru.flink.state.SlidingPriceWindow;
import ru.flink.models.TradePoint;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImpulseStrategyTest {

    private final ImpulseStrategy strategy = new ImpulseStrategy();

    @Test
    void upMove_aboveThreshold_triggers() {
        assertTrue(strategy.trigger(windowWith(100L, 120L), filter(Direction.UP, 10)));
    }

    @Test
    void upMove_belowThreshold_doesNotTrigger() {
        assertFalse(strategy.trigger(windowWith(100L, 105L), filter(Direction.UP, 10)));
    }

    @Test
    void upMove_exactThreshold_triggersBecauseComparisonIsInclusive() {
        assertTrue(strategy.trigger(windowWith(100L, 110L), filter(Direction.UP, 10)));
    }

    @Test
    void downMove_aboveThreshold_triggersForDownFilter() {
        assertTrue(strategy.trigger(windowWith(100L, 80L), filter(Direction.DOWN, 10)));
    }

    //TODO посмотреть тесты
    @Test
    void downMove_belowThreshold_doesNotTrigger() {
        assertFalse(strategy.trigger(windowWith(100L, 95L), filter(Direction.DOWN, 10)));
    }

    //TODO посмотреть тесты
    @Test
    void downMove_exactThreshold_triggersBecauseComparisonIsInclusive() {
        assertTrue(strategy.trigger(windowWith(100L, 90L), filter(Direction.DOWN, 10)));
    }

    @Test
    void downMove_doesNotTriggerForUpFilter() {
        assertFalse(strategy.trigger(windowWith(100L, 80L), filter(Direction.UP, 10)));
    }

    @Test
    void bothDirection_triggersForUpAndDownMoves() {
        assertTrue(strategy.trigger(windowWith(100L, 120L), filter(Direction.BOTH, 10)));
        assertTrue(strategy.trigger(windowWith(100L, 80L), filter(Direction.BOTH, 10)));
    }

    //TODO посмотреть тесты
    @Test
    void bothDirection_belowThresholdDoesNotTrigger() {
        assertFalse(strategy.trigger(windowWith(100L, 105L), filter(Direction.BOTH, 10)));
        assertFalse(strategy.trigger(windowWith(100L, 95L), filter(Direction.BOTH, 10)));
    }

    @Test
    void zeroMin_doesNotTrigger() {
        SlidingPriceWindow window = new SlidingPriceWindow(1_000L);
        window.add(new TradePoint(100L, 0L));
        window.add(new TradePoint(200L, 120L));

        assertFalse(strategy.trigger(window, filter(Direction.BOTH, 10)));
    }

    private static SlidingPriceWindow windowWith(long firstPrice, long secondPrice) {
        SlidingPriceWindow window = new SlidingPriceWindow(1_000L);
        window.add(new TradePoint(100L, firstPrice));
        window.add(new TradePoint(200L, secondPrice));
        return window;
    }

    private static ImpulseRuntimeFilter filter(Direction direction, int percent) {
        var payload = new OutboxPayload.ImpulseFilter(
                Set.of("binance"),
                Set.of("futures"),
                Set.of(),
                60,
                direction,
                percent,
                0
        );
        return new ImpulseRuntimeFilter(1L, payload, Set.of(100L));
    }
}
