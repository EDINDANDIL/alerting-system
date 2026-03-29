package ru.core.trades;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TradePointTest {

    @Test
    void recordCreation_allFieldsAccessible() {
        var point = new TradePoint(1_000_000L, 50_000L);

        assertEquals(1_000_000L, point.timestampNs());
        assertEquals(50_000L, point.priceRaw());
    }

    @Test
    void record_equalsAndHashCode_sameValues() {
        var p1 = new TradePoint(100L, 200L);
        var p2 = new TradePoint(100L, 200L);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void record_notEquals_differentTimestamp() {
        var p1 = new TradePoint(100L, 200L);
        var p2 = new TradePoint(200L, 200L);

        assertNotEquals(p1, p2);
    }

    @Test
    void record_notEquals_differentPrice() {
        var p1 = new TradePoint(100L, 200L);
        var p2 = new TradePoint(100L, 300L);

        assertNotEquals(p1, p2);
    }

    @Test
    void record_toString_containsFields() {
        var point = new TradePoint(999L, 888L);

        String str = point.toString();

        assertEquals("TradePoint[timestampNs=999, priceRaw=888]", str);
    }

    @Test
    void largePriceValues_handledCorrectly() {
        long largePrice = 999_999_999_999L;
        var point = new TradePoint(1_000_000L, largePrice);

        assertEquals(largePrice, point.priceRaw());
    }

    @Test
    void zeroPrice_validPoint() {
        var point = new TradePoint(1_000_000L, 0L);

        assertEquals(0L, point.priceRaw());
    }

    @Test
    void zeroTimestamp_validPoint() {
        var point = new TradePoint(0L, 100L);

        assertEquals(0L, point.timestampNs());
    }
}
