package ru.util;

import org.junit.jupiter.api.Test;
import ru.core.trades.TradePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Дополнительные тесты для {@link SlidingWindow}.
 * Проверяют сложные сценарии использования.
 */
class SlidingWindowExtendedTest {

    private static final long ONE_SECOND_NS = 1_000_000_000L;

    @Test
    void windowWithMultiplePoints_correctMinMaxTracking() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        // Добавляем последовательность: 100, 150, 80, 200, 90
        window.add(tp(100L, 100L));
        window.add(tp(200L, 150L));
        window.add(tp(300L, 80L));
        window.add(tp(400L, 200L));
        window.add(tp(500L, 90L));

        // Максимум 200 (позже), минимум 80 (раньше) -> положительный импульс
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
        assertEquals((200.0 - 80.0) / 80.0, impulse, 1e-9);
    }

    @Test
    void windowWithExpiredPoints_correctRecalculation() {
        long windowNs = 500L;
        var window = new SlidingWindow(windowNs);

        window.add(tp(0L, 100L));
        window.add(tp(200L, 150L));
        window.add(tp(600L, 120L)); // Точка при t=0 истекла

        // Остались точки при t=200 (150) и t=600 (120)
        // Максимум 150 (раньше), минимум 120 (позже) -> отрицательный импульс
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse < 0);
    }

    @Test
    void windowWithEqualPrices_zeroImpulse() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        window.add(tp(100L, 100L));
        window.add(tp(200L, 100L));
        window.add(tp(300L, 100L));

        assertEquals(0.0, window.getCurrentImpulsePercent());
    }

    @Test
    void windowWithIncreasingSequence_positiveImpulse() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        window.add(tp(100L, 100L));
        window.add(tp(200L, 110L));
        window.add(tp(300L, 120L));
        window.add(tp(400L, 130L));

        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
        assertEquals((130.0 - 100.0) / 100.0, impulse, 1e-9);
    }

    @Test
    void windowWithDecreasingSequence_negativeImpulse() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        window.add(tp(100L, 100L));
        window.add(tp(200L, 90L));
        window.add(tp(300L, 80L));
        window.add(tp(400L, 70L));

        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse < 0);
        assertEquals(-(100.0 - 70.0) / 70.0, impulse, 1e-9);
    }

    @Test
    void windowWithVolatilePrices_correctExtremes() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        // Волатильная последовательность
        window.add(tp(100L, 50L));   // min
        window.add(tp(200L, 200L));  // max
        window.add(tp(300L, 75L));
        window.add(tp(400L, 150L));
        window.add(tp(500L, 60L));

        // min=50 (t=100), max=200 (t=200) -> max позже min -> положительный
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
        assertEquals((200.0 - 50.0) / 50.0, impulse, 1e-9);
    }

    @Test
    void windowBoundary_exactCutoff_included() {
        long windowNs = 1000L;
        var window = new SlidingWindow(windowNs);

        window.add(tp(0L, 100L));
        window.add(tp(1000L, 150L)); // Ровно на границе

        // Обе точки должны быть в окне
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
    }

    @Test
    void windowBoundary_justBeforeCutoff_excluded() {
        long windowNs = 1000L;
        var window = new SlidingWindow(windowNs);

        window.add(tp(0L, 100L));
        window.add(tp(1001L, 150L)); // За пределами окна

        // Первая точка должна быть исключена
        double impulse = window.getCurrentImpulsePercent();
        assertEquals(0.0, impulse);
    }

    @Test
    void windowWithLargePriceValues_noOverflow() {
        var window = new SlidingWindow(ONE_SECOND_NS);
        long largePrice1 = 1_000_000_000_000L;
        long largePrice2 = 1_100_000_000_000L;

        window.add(tp(100L, largePrice1));
        window.add(tp(200L, largePrice2));

        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
        assertEquals(0.1, impulse, 1e-9);
    }

    @Test
    void windowWithSmallPriceValues_correctPrecision() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        window.add(tp(100L, 1L));
        window.add(tp(200L, 2L));

        double impulse = window.getCurrentImpulsePercent();
        assertEquals(1.0, impulse, 1e-9); // 100% рост
    }

    @Test
    void window_monotonicMinDeque_maintainsOrder() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        // Добавляем убывающую последовательность - каждый новый элемент меньше предыдущего
        window.add(tp(100L, 100L));
        window.add(tp(200L, 90L));
        window.add(tp(300L, 80L));
        window.add(tp(400L, 70L));

        // В minDeque должен остаться только последний (70), т.к. он меньше всех
        // и все предыдущие были удалены
        window.add(tp(500L, 200L));

        // min=70 (t=400), max=200 (t=500) -> положительный
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse > 0);
    }

    @Test
    void window_monotonicMaxDeque_maintainsOrder() {
        var window = new SlidingWindow(ONE_SECOND_NS);

        // Добавляем возрастающую последовательность
        window.add(tp(100L, 70L));
        window.add(tp(200L, 80L));
        window.add(tp(300L, 90L));
        window.add(tp(400L, 100L));

        // В maxDeque должен остаться только последний (100)
        window.add(tp(500L, 50L));

        // max=100 (t=400), min=50 (t=500) -> отрицательный (max раньше min)
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse < 0);
    }

    @Test
    void window_rapidPriceChanges_stableImpulse() {
        var window = new SlidingWindow(10_000L);

        for (long i = 0; i < 100; i++) {
            long price = 100L + (i % 10);
            window.add(tp(i * 100L, price));
        }

        // После многих изменений импульс должен быть корректным
        double impulse = window.getCurrentImpulsePercent();
        assertTrue(impulse >= -1.0 && impulse <= 1.0);
    }

    private static TradePoint tp(long timestampNs, long priceRaw) {
        return new TradePoint(timestampNs, priceRaw);
    }
}
